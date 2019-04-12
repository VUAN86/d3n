var _ = require('lodash');
var path = require('path');
var async = require('async');
var should = require('should');
var request = require('supertest');
var ProfileData = require('./../config/profile.data.js');
var MediaService = require('./../../../index.js');
var Errors = require('./../../config/errors.js');
var HttpApi = require('./../../api/httpMediaApi.js');
var DataIds = require('./../config/_id.data.js');
var ProtocolMessage = require('nodejs-protocol');
var mediaService = new MediaService();
var UploadApiFactory = require('./../../factories/uploadApiFactory.js');
var Config = require('./../../config/config.js');
var StandardErrors = require('nodejs-errors');
var mlog = require('mocha-logger');

var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeTaskLock = KeyvalueService.Models.AerospikeTaskLock;


// Initialize S3 storage with the configuration settings
var S3Client = require('nodejs-s3client');
var Storage = S3Client.getInstance().Storage;

describe('HTTP Profile Picture API', function () {
    this.timeout(10000);
    describe('POST /updateProfilePicture errors', function () {
        this.timeout(10000);
        it('HTTP 401 ERR_MEDIA_API_PROVIDER_TOKEN_NOT_VALID for missing token', function (done) {
           _apiPostFail(
               'invalid-jwt-token',
               'update',
               'test.jpg',
               _.clone(StandardErrors['ERR_TOKEN_NOT_VALID']),
               done
           );
       });
       it('HTTP 401 ERR_MEDIA_API_PROVIDER_TOKEN_NOT_VALID for invalid token', function (done) {
           _apiPostFail(
               '',
               'update',
               'test.jpg',
               _.clone(StandardErrors['ERR_TOKEN_NOT_VALID']),
               done
           );
       });

       it('HTTP 401 ERR_MEDIA_VALIDATION_FAILED for missing action', function (done) {
           _apiPostFail(
               ProfileData.TEST_TOKEN,
               '',
               'test.jpg',
               _.clone(StandardErrors['ERR_VALIDATION_FAILED']),
               done
           );
       });
       it('HTTP 401 ERR_MEDIA_METADATA_NOT_PROVIDED for invalid action', function (done) {
           _apiPostFail(
               ProfileData.TEST_TOKEN,
               'invalid-action',
               'test.jpg',
               _.clone(StandardErrors['ERR_VALIDATION_FAILED']),
               done
           );
       });

       it('HTTP 401 ERR_VALIDATION_FAILED for not set file', function (done) {
           _apiPostFail(
               ProfileData.TEST_TOKEN,
               'update',
               '',
               _.clone(StandardErrors['ERR_VALIDATION_FAILED']),
               done
           );
       });

       it('HTTP 401 ERR_VALIDATION_FAILED for image of large size', function (done){
           _apiPostFail(
               ProfileData.TEST_TOKEN,
               'update',
               'seahorse.png',
               _.clone(StandardErrors['ERR_VALIDATION_FAILED']),
               done
           );
       });
   });

   describe('POST /updateProfilePicture success', function () {
       
      it('HTTP 200 OK for UPDATE', function (done) {
          this.timeout(30000);

          var responseMedia = {};
          _apiPostSucc(
            ProfileData.TEST_TOKEN,
            'update',
            'test.jpg',
            function(responseContent) {
              should.exists(responseContent);
              responseContent.should.have.property('profile').which.is.a.Object();
              responseContent.profile.id.should.eql(DataIds.TEST_USER_ID + '.jpg');
              responseContent.profile.namespace.should.eql('profile');

              responseMedia.id = responseContent.profile.id;
              responseMedia.namespace = responseContent.profile.namespace;
            },
            function (err) {
                if (err) {
                  console.log("Error api post success ", err);
                } else {
                  // Will use a timeout here to validate that all files are on amazon
                  // Listing sometimes will return less entries even if they were
                  // already pushed to amazon
                  setTimeout(function (err) {

                    // validate that what we uploaded exists on the server
                    var prefix = UploadApiFactory.getMediaPrefix(responseMedia);
                    mlog.log("Checking if all files were pushed to s3 storage");

                    Storage.listObjects({"Prefix": prefix}, function (err, data){
                      var s3filelist = _.map(data.Contents, 'Key');
                      mlog.log("Files on s3 with prefix " + prefix);
                      mlog.log(JSON.stringify(s3filelist, null, 2));

                      var generatedThumbList = _generateThumbnailArr(responseMedia);

                      s3filelist.should.eql(generatedThumbList.sort());

                      mlog.log("Cleanup files from storage ");

                      // also cleanup
                      UploadApiFactory.deleteFromStorage(responseMedia, done);
                    });
                  }, 5000);
                }
            });
       });
       
        it('HTTP 200 OK for UPDATE concurrency', function (done) {
            this.timeout(30000);
            
            var lockTaskId = 'updateProfilePictureTask-' + DataIds.TEST_USER_ID;
            // lock upload profile
            AerospikeTaskLock.lock(lockTaskId, function (err, lock) {
                if(err) {
                    return done(err);
                }
                
                // afer 5 secs unlock upload profile. during 5 secs queue is blocked
                setTimeout(function () {
                    AerospikeTaskLock.remove(lockTaskId, function (errUnlock) {
                        if(errUnlock) {
                            done(errUnlock);
                        }
                    });
                }, 5000);
                
                async.times(5, function (i, cbItem) {
                    var responseMedia = {};
                    _apiPostSucc(
                        ProfileData.TEST_TOKEN,
                        'update',
                        'test.jpg',
                        function(responseContent) {
                            //console.log('>>>>done:', i);
                            should.exists(responseContent);
                            responseContent.should.have.property('profile').which.is.a.Object();
                            responseContent.profile.id.should.eql(DataIds.TEST_USER_ID + '.jpg');
                            responseContent.profile.namespace.should.eql('profile');

                            responseMedia.id = responseContent.profile.id;
                            responseMedia.namespace = responseContent.profile.namespace;
                        },
                        function (err) {
                            return cbItem(err);
                        }
                    );

                }, function (err) {
                    done(err);
                });
                
            });
            
          
       });
        it('HTTP 200 OK for UPDATE concurrency2', function (done) {
            this.timeout(30000);
            
            async.times(5, function (i, cbItem) {
                var responseMedia = {};
                _apiPostSucc(
                    ProfileData.TEST_TOKEN,
                    'update',
                    'test.jpg',
                    function(responseContent) {
                        //console.log('>>>>done:', i);
                        should.exists(responseContent);
                        responseContent.should.have.property('profile').which.is.a.Object();
                        responseContent.profile.id.should.eql(DataIds.TEST_USER_ID + '.jpg');
                        responseContent.profile.namespace.should.eql('profile');

                        responseMedia.id = responseContent.profile.id;
                        responseMedia.namespace = responseContent.profile.namespace;
                    },
                    function (err) {
                        return cbItem(err);
                    }
                );

            }, function (err) {
                done(err);
            });

          
       });
       it('HTTP 200 OK for DELETE', function (done) {
           this.timeout(30000);

           var responseMedia = {};
           _apiPostSucc(
             ProfileData.TEST_TOKEN,
             'delete',
             '',
             function(responseContent) {
               should.exists(responseContent);
               responseContent.should.have.property('profile').which.is.a.Object();
               responseContent.profile.id.should.eql('');
               responseContent.profile.namespace.should.eql('profile');
             },
             done);
        });
   });
});

/*
 * Generate a list of thumbnails according to the configuration
 */
function _generateThumbnailArr(media) {
  var generatedThumbList;
  var fileinfo = path.parse(media.id);

  var params = {
    "id": media.id,
    "namespace": media.namespace,
    "_generatedFileInfo": {
      "basename": fileinfo.name,
      "extension": fileinfo.ext
    },
  }

  UploadApiFactory.thumbnailInfoCompile(params, Config.mediaConfiguration);
  var generatedThumbList = _.map(params._thumbnails, 'filename');
  generatedThumbList.push(UploadApiFactory.getMediaKey(media));

  return generatedThumbList;
}


function _apiPostSucc(token, action, mediaFile, contentCallback, doneCallback) {
  try {
      var apiReq = request(mediaService.httpProvider())
                    .post('/updateProfilePicture')
                    .field('token', token)
                    .field('action', action)
                    .set('Accept', 'application/json');

      if (mediaFile) {
          apiReq = apiReq.attach('media_file', path.join(__dirname, '..', 'resources/' + mediaFile))
      }

      apiReq.expect(200)
          .expect('Content-Type', /application\/json/)
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

function _apiPostFail(token, action, mediaFile, error, doneCallback) {
  try {
    var apiReq = request(mediaService.httpProvider()).post('/updateProfilePicture');

    if (token) {
        apiReq = apiReq.field('token', token);
    }
    if (action) {
        apiReq = apiReq.field('action', action);
    }
    if (mediaFile) {
        apiReq = apiReq.attach('media_file', path.join(__dirname, '..', 'resources/' + mediaFile))
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
