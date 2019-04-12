var _ = require('lodash');
var path = require('path');
var fs = require('fs');
var async = require('async');
var should = require('should');
var request = require('supertest');
var md5File = require('md5-file')
var uuid = require('uuid');
var ProfileData = require('./../config/profile.data.js');
var MediaService = require('./../../../index.js');
var ProtocolMessage = require('nodejs-protocol');
var mediaService = new MediaService();
var UploadApiFactory = require('./../../factories/uploadApiFactory.js');
var Config = require('./../../config/config.js');
var StandardErrors = require('nodejs-errors');
var mlog = require('mocha-logger');

// Initialize S3 storage with the configuration settings
var S3Client = require('nodejs-s3client');
var SecureStorage = S3Client.getInstance().SecureStorage;

describe('HTTP Secure Document API', function () {
    this.timeout(10000);
    describe('POST /addDocument errors', function () {
        this.timeout(10000);
        it('HTTP 401 ERR_TOKEN_NOT_VALID for missing token', function (done) {
           _apiPostFail(
               '',
               'test.jpg',
               _.clone(StandardErrors['ERR_TOKEN_NOT_VALID']),
               done
           );
       });
       it('HTTP 401 ERR_TOKEN_NOT_VALID for invalid token', function (done) {
           _apiPostFail(
               'invalid-jwt-token',
               'test.jpg',
               _.clone(StandardErrors['ERR_TOKEN_NOT_VALID']),
               done
           );
       });

       it('HTTP 401 ERR_VALIDATION_FAILED for not set file', function (done) {
           _apiPostFail(
               ProfileData.TEST_TOKEN,
               '',
               _.clone(StandardErrors['ERR_VALIDATION_FAILED']),
               done
           );
       });
   });

   describe('POST /addDocument success', function () {
      it('HTTP 200 OK', function (done) {
          this.timeout(30000);

          var responseDocumentKey = '';
          _apiPostSucc(
              ProfileData.TEST_TOKEN,
              'test.jpg',
              function(responseContent) {
                should.exists(responseContent);
                responseContent.should.have.property('document').which.is.a.Object();
                responseDocumentKey = responseContent.document.key;
                mlog.log("Completed document save process to s3, key: ", responseDocumentKey);
              },
              function (err) {
                if (err) {
                  console.log("Error api post success ", err);
                  done(err);
                } else {
                    // Will use a timeout here to validate that all files are on amazon
                    // Listing sometimes will return less entries even if they were
                    // already pushed to amazon
                    
                    var originalFile = path.join(__dirname, '..', 'resources/' + 'test.jpg');
                    var originalFileChecksum = '';
                    var s3File = path.join(__dirname, '..', 'resources/' + uuid.v4());
                    var s3FileChecksum = '';
                    mlog.log("Retrieve file from s3 storage " + responseDocumentKey + " and storing it to " + s3File);
                    SecureStorage.getObject({objectId: responseDocumentKey, outFile: s3File}, function(err) {
                        originalFileChecksum = md5File.sync(originalFile);
                        s3FileChecksum = md5File.sync(s3File);
                        
                        mlog.log("Comparing checksum for the two files: \n\t\t" + originalFileChecksum + "\n\t\t" + s3FileChecksum);

                        originalFileChecksum.should.eql(s3FileChecksum);

                        mlog.log("Cleanup file from local disk and storage ");

                        fs.unlink(s3File, function(err) {
                            if (err) {
                                mlog.log("Error cleaning up file local disk ", err);
                                done(err);
                            }
                            SecureStorage.deleteObjects({objectKeys: [responseDocumentKey]}, done);
                        });
                    })
                }
              });
       });
   });
});

function _apiPostSucc(token, documentFile, contentCallback, doneCallback) {
   try {
       request(mediaService.httpProvider())
           .post('/addDocument')
           .field('token', token)
           .attach('file_content', path.join(__dirname, '..', 'resources/' + documentFile))
           .set('Accept', 'application/json')
           .expect('Content-Type', /application\/json/)
           .expect(200)
           .end(function (err, res) {
               if (err) {
                  return doneCallback(err);
               }
               var message = new ProtocolMessage(JSON.parse(res.text));
               message.isValid('websocketMessage').should.be.eql(true);
               if (contentCallback) {
                   contentCallback(message.getContent());
               }
               return doneCallback();
           });
   } catch (ex) {
       return doneCallback(ex);
   }
}

function _apiPostFail(token, documentFile, error, doneCallback) {
  try {
    var apiReq = request(mediaService.httpProvider()).post('/addDocument');
    if (token) {
        apiReq = apiReq.field('token', token);
    }
    if (documentFile) {
        apiReq = apiReq.attach('file_content', path.join(__dirname, '..', 'resources/' + documentFile))
    }

    apiReq.expect('Content-Type', /application\/json/)
        .expect(401)
        .end(function (err, res) {
            if (err) {
                return doneCallback(err);
            }
           var message = new ProtocolMessage(JSON.parse(res.text));
           message.isValid('websocketMessage').should.be.eql(true);
           error.should.be.eql(message.getError());
           return doneCallback();
        });
    } catch (ex) {
       return doneCallback(ex);
    }
}
