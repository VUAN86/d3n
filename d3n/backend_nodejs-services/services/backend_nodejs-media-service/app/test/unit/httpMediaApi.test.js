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

// Initialize S3 storage with the configuration settings
var S3Client = require('nodejs-s3client');
var Storage = S3Client.getInstance().Storage;

describe('HTTP Media API', function () {
    this.timeout(10000);
    describe('POST /addMedia errors', function () {
        this.timeout(10000);
        it('HTTP 401 ERR_TOKEN_NOT_VALID for missing token', function (done) {
           _apiPostFail(
               'invalid-jwt-token',
               'image',
               JSON.stringify({"type":"image/jpg"}),
               'app',
               'test.jpg',
               _.clone(StandardErrors['ERR_TOKEN_NOT_VALID']),
               done
           );
       });
       it('HTTP 401 ERR_TOKEN_NOT_VALID for invalid token', function (done) {
           _apiPostFail(
               '',
               'image',
               JSON.stringify({"type":"image/jpg"}),
               'app',
               'test.jpg',
               _.clone(StandardErrors['ERR_TOKEN_NOT_VALID']),
               done
           );
       });

       it('HTTP 401 ERR_VALIDATION_FAILED for missing content type', function (done) {
           _apiPostFail(
               ProfileData.TEST_TOKEN,
               '',
               JSON.stringify({"type":"image/jpg"}),
               'app',
               'test.jpg',
               _.clone(StandardErrors['ERR_VALIDATION_FAILED']),
               done
           );
       });
       it('HTTP 401 ERR_VALIDATION_FAILED for invalid content type', function (done) {
           _apiPostFail(
               ProfileData.TEST_TOKEN,
               'not-an-image',
               JSON.stringify({"type":"image/jpg"}),
               'app',
               'test.jpg',
               _.clone(StandardErrors['ERR_VALIDATION_FAILED']),
               done
           );
       });

       it('HTTP 401 ERR_VALIDATION_FAILED for missing namespace', function (done) {
           _apiPostFail(
               ProfileData.TEST_TOKEN,
               '',
               JSON.stringify({"type":"image/jpg"}),
               '',
               'test.jpg',
               _.clone(StandardErrors['ERR_VALIDATION_FAILED']),
               done
           );
       });
       it('HTTP 401 ERR_VALIDATION_FAILED for invalid namespace', function (done) {
           _apiPostFail(
               ProfileData.TEST_TOKEN,
               'not-an-image',
               JSON.stringify({"type":"image/jpg"}),
               'no-good-namespace',
               'test.jpg',
               _.clone(StandardErrors['ERR_VALIDATION_FAILED']),
               done
           );
       });

       it('HTTP 401 ERR_VALIDATION_FAILED for missing metadata', function (done) {
           _apiPostFail(
               ProfileData.TEST_TOKEN,
               'image',
               '',
               'app',
               'test.jpg',
               _.clone(StandardErrors['ERR_VALIDATION_FAILED']),
               done
           );
       });
       it('HTTP 401 ERR_VALIDATION_FAILED for invalid metadata', function (done) {
           _apiPostFail(
               ProfileData.TEST_TOKEN,
               'image',
               'invalid"json"string',
               'app',
               'test.jpg',
               _.clone(StandardErrors['ERR_VALIDATION_FAILED']),
               done
           );
       });

       it('HTTP 401 ERR_VALIDATION_FAILED for not set file', function (done) {
           _apiPostFail(
               ProfileData.TEST_TOKEN,
               'image',
               JSON.stringify({"type":"image/jpg"}),
               'app',
               '',
               _.clone(StandardErrors['ERR_VALIDATION_FAILED']),
               done
           );
       });
   });

   describe('POST /addMedia success', function () {
      it('HTTP 200 OK', function (done) {
          this.timeout(30000);

          var responseMedia = {};
          _apiPostSucc( {
              'token': ProfileData.TEST_TOKEN,
              'contentType': 'image',
              'metaData': JSON.stringify({"type":"image/jpg"}),
              'namespace': 'voucher',
              'parentId': DataIds.MEDIA_1_ID,
              'workorderId': DataIds.WORKORDER_1_ID,
              'mediaFile': 'test.jpg',
              'title': 'title',
              'type': 'original',
              'description': 'description',
              'source': 'source',
              'copyright': 'copyright',
              'licence': 'licence',
              'usageInformation': 'usageInformation'
            }, function(responseContent) {
                should.exists(responseContent);
                responseContent.should.have.property('media').which.is.a.Object();
                responseContent.media.metaData.should.eql({"type":"image/jpg"});
                responseContent.media.namespace.should.eql('voucher');
                responseContent.media.workorderId.should.eql(DataIds.WORKORDER_1_ID.toString());
                responseContent.media.title.should.eql('title');
                responseContent.media.type.should.eql('original');
                responseContent.media.description.should.eql('description');
                responseContent.media.source.should.eql('source');
                responseContent.media.parentId.should.eql(DataIds.MEDIA_1_ID.toString());
                responseContent.media.copyright.should.eql('copyright');
                responseContent.media.licence.should.eql('licence');
                responseContent.media.usageInformation.should.eql('usageInformation');
                responseMedia = responseContent.media;
                mlog.log("Completed image save process", responseContent.media.id);
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
                      generatedThumbList.push(prefix + ".metadata.json");

                      s3filelist.should.eql(generatedThumbList.sort());

                      mlog.log("Cleanup files from storage ");

                      // also cleanup
                      UploadApiFactory.deleteFromStorage(responseMedia, done);
                    });
                  }, 5000);
                }
              });
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

function _apiPostSucc(data, contentCallback, doneCallback) {
   try {
       request(mediaService.httpProvider())
           .post('/addMedia')
           .field('content_type', data.contentType)
           .field('meta_data', data.metaData)
           .field('token', data.token)
           .field('workorder_id', data.workorderId)
           .field('parent_id', data.parentId)
           .field('namespace', data.namespace)
           .field('type', data.type)
           .field('title', data.title)
           .field('description', data.description)
           .field('source', data.source)
           .field('copyright', data.copyright)
           .field('licence', data.licence)
           .field('usage_information', data.usageInformation)
           .attach('media_file', path.join(__dirname, '..', 'resources/' + data.mediaFile))
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

function _apiPostFail(token, contentType, metaData, namespace, mediaFile, error, doneCallback) {
  try {
    var apiReq = request(mediaService.httpProvider()).post('/addMedia');
    if (token) {
        apiReq = apiReq.field('token', token);
    }
    if (namespace) {
        apiReq = apiReq.field('namespace', namespace);
    }
    if (contentType) {
        apiReq = apiReq.field('content_type', contentType);
    }
    if (metaData) {
        apiReq = apiReq.field('meta_data', metaData);
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
