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

describe('HTTP Group Picture API', function () {
    this.timeout(10000);
    describe('POST /updateGroupPicture errors', function () {
        this.timeout(10000);
        it('HTTP 401 ERR_MEDIA_API_PROVIDER_TOKEN_NOT_VALID for missing token', function (done) {
           _apiPostFail(
               'invalid-jwt-token',
               DataIds.GROUP_1_ID,
               'update',
               DataIds.TENANT_1_ID,
               'test.jpg',
               _.clone(StandardErrors['ERR_TOKEN_NOT_VALID']),
               done
           );
       });
       it('HTTP 401 ERR_MEDIA_API_PROVIDER_TOKEN_NOT_VALID for invalid token', function (done) {
           _apiPostFail(
               '',
               DataIds.GROUP_1_ID,
               'update',
               DataIds.TENANT_1_ID,
               'test.jpg',
               _.clone(StandardErrors['ERR_TOKEN_NOT_VALID']),
               done
           );
       });

       it('HTTP 401 ERR_MEDIA_VALIDATION_FAILED for missing group', function (done) {
           _apiPostFail(
               ProfileData.TEST_TOKEN,
               '',
               'update',
               DataIds.TENANT_1_ID,
               'test.jpg',
               _.clone(StandardErrors['ERR_VALIDATION_FAILED']),
               done
           );
       });
       it('HTTP 401 ERR_MEDIA_METADATA_NOT_PROVIDED for invalid groupId', function (done) {
           _apiPostFail(
               ProfileData.TEST_TOKEN,
               'not-found-group-id',
               'invalid-action',
               DataIds.TENANT_1_ID,
               'test.jpg',
               _.clone(StandardErrors['ERR_VALIDATION_FAILED']),
               done
           );
       });

       it('HTTP 401 ERR_MEDIA_VALIDATION_FAILED for missing tenantId', function (done) {
           _apiPostFail(
               ProfileData.TEST_TOKEN,
               DataIds.GROUP_1_ID,
               'update',
               '',
               'test.jpg',
               _.clone(StandardErrors['ERR_VALIDATION_FAILED']),
               done
           );
       });

       it('HTTP 401 ERR_MEDIA_VALIDATION_FAILED for missing action', function (done) {
           _apiPostFail(
               ProfileData.TEST_TOKEN,
               DataIds.GROUP_1_ID,
               '',
               DataIds.TENANT_1_ID,
               'test.jpg',
               _.clone(StandardErrors['ERR_VALIDATION_FAILED']),
               done
           );
       });
       it('HTTP 401 ERR_MEDIA_METADATA_NOT_PROVIDED for invalid action', function (done) {
           _apiPostFail(
               ProfileData.TEST_TOKEN,
               DataIds.GROUP_1_ID,
               'invalid-action',
               DataIds.TENANT_1_ID,
               'test.jpg',
               _.clone(StandardErrors['ERR_VALIDATION_FAILED']),
               done
           );
       });

       it('HTTP 401 ERR_VALIDATION_FAILED for not set file', function (done) {
           _apiPostFail(
               ProfileData.TEST_TOKEN,
               DataIds.GROUP_1_ID,
               'update',
               DataIds.TENANT_1_ID,
               '',
               _.clone(StandardErrors['ERR_VALIDATION_FAILED']),
               done
           );
       });

       it('HTTP 401 ERR_VALIDATION_FAILED for image of large size', function (done){
           _apiPostFail(
               ProfileData.TEST_TOKEN,
               DataIds.GROUP_1_ID,
               'update',
               DataIds.TENANT_1_ID,
               'seahorse.png',
               _.clone(StandardErrors['ERR_VALIDATION_FAILED']),
               done
           );
       });
   });

   describe('POST /updateGroupPicture success', function () {
      it('HTTP 200 OK for UPDATE', function (done) {
          this.timeout(30000);

          var responseMedia = {};
          _apiPostSucc(
            ProfileData.TEST_TOKEN,
            DataIds.GROUP_1_ID,
            'update',
            DataIds.TENANT_1_ID,
            'test.jpg',
            function(responseContent) {
              should.exists(responseContent);
              responseContent.should.have.property('group').which.is.a.Object();
              responseContent.group.should.have.property('image').which.is.a.String();

              var resp = responseContent.group.image.split('/');
              resp.should.be.an.Array().with.lengthOf(2);
              responseMedia.namespace = resp[0];
              responseMedia.id = resp[1];
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
       it('HTTP 200 OK for DELETE', function (done) {
           this.timeout(30000);

           var responseMedia = {};
           _apiPostSucc(
             ProfileData.TEST_TOKEN,
             DataIds.GROUP_1_ID,
             'delete',
             DataIds.TENANT_1_ID,
             '',
             function(responseContent) {
               should.exists(responseContent);
               responseContent.should.have.property('group').which.is.a.Object();
               responseContent.group.should.have.property('image').which.is.a.String();
               responseContent.group.image.should.eql('');
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


function _apiPostSucc(token, groupId, action, tenantId, mediaFile, contentCallback, doneCallback) {
  try {
      var apiReq = request(mediaService.httpProvider())
                    .post('/updateGroupPicture')
                    .field('token', token)
                    .field('groupId', groupId)
                    .field('action', action)
                    .field('tenantId', tenantId)
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

function _apiPostFail(token, groupId, action, tenantId, mediaFile, error, doneCallback) {
  try {
    var apiReq = request(mediaService.httpProvider()).post('/updateGroupPicture');

    if (token) {
        apiReq = apiReq.field('token', token);
    }
    if (action) {
        apiReq = apiReq.field('action', action);
    }
    if (groupId) {
        apiReq = apiReq.field('groupId', groupId);
    }
    if (tenantId) {
        apiReq = apiReq.field('tenantId', tenantId);
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
