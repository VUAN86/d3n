var _ = require('lodash');
var fs = require('fs');
var path = require('path');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var Database = require('nodejs-database').getInstance(Config);
var DataIds = require('./../config/_id.data.js');
var Data = require('./../config/media.data.js');
var ProfileData = require('./../config/profile.data.js');
var StandardErrors = require('nodejs-errors');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var mlog = require('mocha-logger');

// Initialize S3 storage with the configuration settings
var UploadApiFactory = require('./../../factories/uploadApiFactory.js');
var S3Client = require('nodejs-s3client');
var Storage = S3Client.getInstance().Storage;

describe('WS Media API', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {

        describe('[' + serie + '] ' + 'mediaGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: media:mediaGet', function (done) {
                var content = { id: DataIds.MEDIA_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.MEDIA_1);
                    shouldResponseContent.tags = ShouldHelper.treatAsList(Data.MEDIA_1.tags, 0, 0);
                    shouldResponseContent.poolsIds = ShouldHelper.treatAsList(Data.MEDIA_1.poolsIds, 0, 0);
                    shouldResponseContent.submediasIds = ShouldHelper.treatAsList(Data.MEDIA_1.submediasIds, 0, 0);
                    ShouldHelper.deepEqual(responseContent.media, shouldResponseContent);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: media:mediaGet and related objects', function (done) {
                var content = { id: DataIds.MEDIA_2_ID };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.MEDIA_2);
                    shouldResponseContent.tags = ShouldHelper.treatAsList(Data.MEDIA_1.tags, 0, 0);
                    shouldResponseContent.poolsIds = ShouldHelper.treatAsList(Data.MEDIA_2.poolsIds, 0, 0);
                    shouldResponseContent.submediasIds = ShouldHelper.treatAsList(Data.MEDIA_2.submediasIds, 0, 0);
                    ShouldHelper.deepEqual(responseContent.media, shouldResponseContent);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE: media:mediaGet media of a different tenant', function (done) {
                var content = { id: DataIds.MEDIA_3_ID };
                global.wsHelper.apiSecureFail(serie, 'media/mediaGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
            it('[' + serie + '] ' + 'FAILURE: media:mediaGet media that belongs to any tenant', function (done) {
                var content = { id: DataIds.MEDIA_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'media/mediaGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: media:mediaGet', function (done) {
                var content = { id: DataIds.MEDIA_TEST_ID, metaData: {}, contentType: 'image' };
                global.wsHelper.apiSecureFail(serie, 'media/mediaGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'mediaUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: media:mediaUpdate', function (done) {
                var content = {
                    id: DataIds.MEDIA_1_ID,
                    workorderId: DataIds.WORKORDER_1_ID,
                    title: 'new title',
                    type: 'mask',
                    description: 'Yeah, my bad',
                    source: 'New source',
                    copyright: 'Updated copyright',
                    licence: null,
                    usageInformation: 'New usage',
                    metaData: { 'type': 'updated' }
                };
                var resultingMedia = {};
                global.wsHelper.apiSecureSucc(serie, 'media/mediaUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.assign(_.clone(Data.MEDIA_1), content);
                    shouldResponseContent.tags = ShouldHelper.treatAsList(Data.MEDIA_1.tags, 0, 0);
                    shouldResponseContent.poolsIds = ShouldHelper.treatAsList(Data.MEDIA_1.poolsIds, 0, 0);
                    shouldResponseContent.submediasIds = ShouldHelper.treatAsList(Data.MEDIA_1.submediasIds, 0, 0);
                    ShouldHelper.deepEqual(responseContent.media, shouldResponseContent);

                    resultingMedia = responseContent.media;
                },
                    function (err, data) {
                        if (err) {
                            return setImmediate(done, err);
                        }
                        setImmediate(function (done) {
                            Storage.getObjectAsBuffer({ "objectId": UploadApiFactory.getMediaPrefix(resultingMedia) + ".metadata.json" }, function (err, data) {
                                try {
                                    var metaData = JSON.parse(data);
                                    should(content.metaData).be.eql(metaData.metaData);
                                    should(content.title).be.eql(metaData.title);
                                    should(content.description).be.eql(metaData.description);
                                    should(content.source).be.eql(metaData.source);
                                    should(content.copyright).be.eql(metaData.copyright);
                                    should(content.licence).be.eql(metaData.licence);
                                    should(content.usageInformation).be.eql(metaData.usageInformation);
                                    return done();
                                } catch (e) {
                                    done(e);
                                }
                            });
                        }, done);
                    }, done);
            });

            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_ID: media:mediaUpdate', function (done) {
                var content = { id: DataIds.MEDIA_TEST_ID, workorderId: null, metaData: {} };
                global.wsHelper.apiSecureFail(serie, 'media/mediaUpdate', ProfileData.CLIENT_INFO_1, content, _.clone(StandardErrors['ERR_DATABASE_NO_RECORD_ID']), done);
            });
        });

        describe('[' + serie + '] ' + 'mediaUpdate - parentId', function () {
            it('[' + serie + '] ' + 'SUCCESS: update parentId', function (done) {
                var content = {
                    id: DataIds.MEDIA_1_ID,
                    parentId: DataIds.MEDIA_NO_DEPENDENCIES_ID
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent.media.id).be.eql(content.id);
                    should(responseContent.media.parentId).be.eql(content.parentId);
                }, done);
            });

            it('[' + serie + '] ' + 'SUCCESS: unset parentId', function (done) {
                var content = {
                    id: DataIds.MEDIA_2_ID,
                    parentId: null
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent.media.id).be.eql(content.id);
                    should.not.exist(responseContent.media.parentId);
                }, done);
            });

            it('[' + serie + '] ' + 'FAILURE ERR_FATAL_ERROR: media:mediaUpdate invalid parentId', function (done) {
                var content = { id: DataIds.MEDIA_1_ID, parentId: 'non-existing-id', workorderId: null, metaData: {} };
                global.wsHelper.apiSecureFail(serie, 'media/mediaUpdate', ProfileData.CLIENT_INFO_1, content, _.clone(StandardErrors['ERR_FATAL_ERROR']), done);
            });
        });

        describe('[' + serie + '] ' + 'mediaUpdate - workorderId', function () {
            it('[' + serie + '] ' + 'SUCCESS: update workorderId', function (done) {
                var content = {
                    id: DataIds.MEDIA_1_ID,
                    workorderId: DataIds.WORKORDER_2_ID
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent.media.id).be.eql(content.id);
                    should(responseContent.media.workorderId).be.eql(content.workorderId);
                }, done);
            });

            it('[' + serie + '] ' + 'SUCCESS: unset workorderId', function (done) {
                var content = {
                    id: DataIds.MEDIA_1_ID,
                    workorderId: null
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent.media.id).be.eql(content.id);
                    should.not.exist(responseContent.media.workorderId);
                }, done);
            });

            it('[' + serie + '] ' + 'ERROR: return fatal error for update with invalid workorderId values ', function (done) {
                var content = {
                    id: DataIds.MEDIA_1_ID,
                    workorderId: DataIds.WORKORDER_TEST_ID
                };

                global.wsHelper.apiSecureFail(serie, 'media/mediaUpdate', ProfileData.CLIENT_INFO_1, content, Errors.MediaApi.FatalError, done);
            });
        });

        describe('[' + serie + '] ' + 'mediaUpdate - tags', function () {
            it('[' + serie + '] ' + 'SUCCESS: add tags', function (done) {
                var content = {
                    id: DataIds.MEDIA_1_ID,
                    tags: [DataIds.TAG_1_TAG]
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent.media.id).be.eql(content.id);
                    should(responseContent.media.tags.total).be.eql(1);
                    should(responseContent.media.tags.items).be.eql(content.tags);
                }, done);
            });

            it('[' + serie + '] ' + 'SUCCESS: update tags', function (done) {
                var content = {
                    id: DataIds.MEDIA_2_ID,
                    tags: [DataIds.TAG_2_TAG]
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent.media.id).be.eql(content.id);
                    should(responseContent.media.tags.total).be.eql(1);
                    should(responseContent.media.tags.items).be.eql(content.tags);
                }, done);
            });

            it('[' + serie + '] ' + 'SUCCESS: update with new tag value ', function (done) {
                var content = {
                    id: DataIds.MEDIA_1_ID,
                    tags: [DataIds.TAG_TEST_TAG]
                };

                global.wsHelper.apiSecureSucc(serie, 'media/mediaUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent.media.id).be.eql(content.id);
                    should(responseContent.media.tags.total).be.eql(1);
                    should(responseContent.media.tags.items).be.eql(content.tags);
                }, done);
            });

        });

        describe('[' + serie + '] ' + 'mediaDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: media:mediaDelete', function (done) {
                var content = { id: DataIds.MEDIA_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaDelete', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.not.exist(responseContent);
                }, done);
            });

            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: mediaDelete', function (done) {
                var content = { id: DataIds.MEDIA_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'media/mediaDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'mediaList', function () {
            it('[' + serie + '] ' + 'SUCCESS: list all', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {},
                    orderBy: [{ field: 'id', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.offset.should.eql(0);
                    responseContent.total.should.eql(2);
                    var shouldResponseContent = _.cloneDeep([Data.MEDIA_1, Data.MEDIA_2]);
                    _.forEach(shouldResponseContent, function (mediaContent) {
                        mediaContent.tags = ShouldHelper.treatAsList(mediaContent.tags);
                        mediaContent.poolsIds = ShouldHelper.treatAsList(mediaContent.poolsIds);
                        mediaContent.submediasIds = ShouldHelper.treatAsList(mediaContent.submediasIds);
                    });
                    ShouldHelper.deepEqualList(shouldResponseContent, responseContent.items);
                }, done);
            });

            it('[' + serie + '] ' + 'SUCCESS: search by id', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.MEDIA_2_ID },
                    orderBy: [{ field: 'id', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.total.should.eql(1);
                    var shouldResponseContent = _.clone(Data.MEDIA_2);
                    shouldResponseContent.tags = ShouldHelper.treatAsList(Data.MEDIA_2.tags);
                    shouldResponseContent.poolsIds = ShouldHelper.treatAsList(Data.MEDIA_2.poolsIds);
                    shouldResponseContent.submediasIds = ShouldHelper.treatAsList(Data.MEDIA_2.submediasIds);

                    ShouldHelper.deepEqual(responseContent.items[0], shouldResponseContent);
                }, done);
            });

            it('[' + serie + '] ' + 'SUCCESS: search by id media of a different tenant', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.MEDIA_3_ID },
                    orderBy: [{ field: 'id', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.total.should.eql(0);
                }, done);
            });

            it('[' + serie + '] ' + 'SUCCESS: mediaList search by contentType', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { contentType: Data.MEDIA_1.contentType },
                    orderBy: [{ field: 'id', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.total.should.eql(1);
                    var shouldResponseContent = _.clone(Data.MEDIA_1);
                    shouldResponseContent.tags = ShouldHelper.treatAsList(Data.MEDIA_1.tags);
                    shouldResponseContent.poolsIds = ShouldHelper.treatAsList(Data.MEDIA_1.poolsIds);
                    shouldResponseContent.submediasIds = ShouldHelper.treatAsList(Data.MEDIA_1.submediasIds);
                    ShouldHelper.deepEqual(responseContent.items[0], shouldResponseContent);
                }, done);
            });

            it('[' + serie + '] ' + 'SUCCESS: mediaList search by type', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { type: 'mask' },
                    orderBy: [{ field: 'id', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.total.should.eql(1);
                    var shouldResponseContent = _.clone(Data.MEDIA_2);
                    shouldResponseContent.tags = ShouldHelper.treatAsList(Data.MEDIA_2.tags);
                    shouldResponseContent.poolsIds = ShouldHelper.treatAsList(Data.MEDIA_2.poolsIds);
                    shouldResponseContent.submediasIds = ShouldHelper.treatAsList(Data.MEDIA_2.submediasIds);
                    ShouldHelper.deepEqual(responseContent.items[0], shouldResponseContent);
                }, done);
            });

            it('[' + serie + '] ' + 'SUCCESS: mediaList search by metaData', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { metaData: '%img/jpg%' },
                    orderBy: [{ field: 'id', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.total.should.eql(1);
                    var shouldResponseContent = _.clone(Data.MEDIA_2);
                    shouldResponseContent.tags = ShouldHelper.treatAsList(Data.MEDIA_2.tags);
                    shouldResponseContent.poolsIds = ShouldHelper.treatAsList(Data.MEDIA_2.poolsIds);
                    shouldResponseContent.submediasIds = ShouldHelper.treatAsList(Data.MEDIA_2.submediasIds);
                    ShouldHelper.deepEqual(responseContent.items[0], shouldResponseContent);
                }, done);
            });

            it('[' + serie + '] ' + 'SUCCESS: list - google like search', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { fulltext: 'Aaaa Bbbb' },
                    orderBy: [{ field: 'id', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.offset.should.eql(0);
                    responseContent.total.should.eql(2);
                    var shouldResponseContent = _.cloneDeep([Data.MEDIA_1, Data.MEDIA_2]);
                    _.forEach(shouldResponseContent, function (mediaContent) {
                        mediaContent.tags = ShouldHelper.treatAsList(mediaContent.tags);
                        mediaContent.poolsIds = ShouldHelper.treatAsList(mediaContent.poolsIds);
                        mediaContent.submediasIds = ShouldHelper.treatAsList(mediaContent.submediasIds);
                    });
                    ShouldHelper.deepEqualList(shouldResponseContent, responseContent.items);
                }, done);
            });

            it('[' + serie + '] ' + 'SUCCESS: mediaList - 0 records', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.MEDIA_TEST_ID },
                    orderBy: [{ field: 'id', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaList', ProfileData.CLIENT_INFO_1, content, function (receivedContent) {
                    should.exist(receivedContent);
                    should(receivedContent).has.property('items').which.has.length(0);
                }, done);
            });

            it('[' + serie + '] ' + 'FAILURE ERR_VALIDATION_FAILED: mediaList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {},
                    orderBy: [{ field: 'userId', direction: 'asc' }]
                };
                global.wsHelper.apiSecureFail(serie, 'media/mediaList', ProfileData.CLIENT_INFO_1, content, Errors.MediaApi.ValidationFailed, done);
            });
        });

        describe('[' + serie + '] ' + 'mediaList search by related objects', function () {
            it('[' + serie + '] ' + 'SUCCESS: tag', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { tags: [DataIds.TAG_1_TAG] },
                    orderBy: [{ field: 'id', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.offset.should.eql(0);
                    responseContent.total.should.eql(2);
                    var shouldResponseContent = _.cloneDeep([Data.MEDIA_1, Data.MEDIA_2]);
                    _.forEach(shouldResponseContent, function (mediaContent) {
                        mediaContent.tags = ShouldHelper.treatAsList(mediaContent.tags);
                        mediaContent.poolsIds = ShouldHelper.treatAsList(mediaContent.poolsIds);
                        mediaContent.submediasIds = ShouldHelper.treatAsList(mediaContent.submediasIds);
                    });
                    ShouldHelper.deepEqualList(shouldResponseContent, responseContent.items);
                }, done);
            });

            it('[' + serie + '] ' + 'SUCCESS: poolIds', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { poolsIds: [DataIds.POOL_1_ID] },
                    orderBy: [{ field: 'id', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.offset.should.eql(0);
                    responseContent.total.should.eql(2);
                    var shouldResponseContent = _.cloneDeep([Data.MEDIA_1, Data.MEDIA_2]);
                    _.forEach(shouldResponseContent, function (mediaContent) {
                        mediaContent.tags = ShouldHelper.treatAsList(mediaContent.tags);
                        mediaContent.poolsIds = ShouldHelper.treatAsList(mediaContent.poolsIds);
                        mediaContent.submediasIds = ShouldHelper.treatAsList(mediaContent.submediasIds);
                    });
                    ShouldHelper.deepEqualList(shouldResponseContent, responseContent.items);
                }, done);
            });

            it('[' + serie + '] ' + 'SUCCESS: submediasIds', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { submediasIds: [DataIds.MEDIA_2_ID] },
                    orderBy: [{ field: 'id', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.offset.should.eql(0);
                    responseContent.total.should.eql(1);
                    var shouldResponseContent = [_.clone(Data.MEDIA_1)];
                    _.forEach(shouldResponseContent, function (mediaContent) {
                        mediaContent.tags = ShouldHelper.treatAsList(mediaContent.tags);
                        mediaContent.poolsIds = ShouldHelper.treatAsList(mediaContent.poolsIds);
                        mediaContent.submediasIds = ShouldHelper.treatAsList(mediaContent.submediasIds);
                    });
                    ShouldHelper.deepEqualList(shouldResponseContent, responseContent.items);
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'mediaList search _allInfo field', function () {
            it('[' + serie + '] ' + 'SUCCESS search by [A32HDE]', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { '_allInfo': 'A32HDE' },
                    orderBy: [{ field: 'id', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.offset.should.eql(0);
                    responseContent.total.should.eql(1);
                    var shouldResponseContent = [_.clone(Data.MEDIA_1)];
                    _.forEach(shouldResponseContent, function (mediaContent) {
                        mediaContent.tags = ShouldHelper.treatAsList(mediaContent.tags);
                        mediaContent.poolsIds = ShouldHelper.treatAsList(mediaContent.poolsIds);
                        mediaContent.submediasIds = ShouldHelper.treatAsList(mediaContent.submediasIds);
                    });
                    ShouldHelper.deepEqualList(shouldResponseContent, responseContent.items);
                }, done);
            });

            it('[' + serie + '] ' + 'SUCCESS search by [media]', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { '_allInfo': 'media' },
                    orderBy: [{ field: 'id', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.offset.should.eql(0);
                    responseContent.total.should.eql(1);
                    var shouldResponseContent = _.cloneDeep([Data.MEDIA_2]);
                    _.forEach(shouldResponseContent, function (mediaContent) {
                        mediaContent.tags = ShouldHelper.treatAsList(mediaContent.tags);
                        mediaContent.poolsIds = ShouldHelper.treatAsList(mediaContent.poolsIds);
                        mediaContent.submediasIds = ShouldHelper.treatAsList(mediaContent.submediasIds);
                    });
                    ShouldHelper.deepEqualList(shouldResponseContent, responseContent.items);
                }, done);
            });

            it('[' + serie + '] ' + 'SUCCESS search by [MIT]', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { '_allInfo': 'MIT' },
                    orderBy: [{ field: 'id', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.offset.should.eql(0);
                    responseContent.total.should.eql(1);
                    var shouldResponseContent = _.cloneDeep([Data.MEDIA_2]);
                    _.forEach(shouldResponseContent, function (mediaContent) {
                        mediaContent.tags = ShouldHelper.treatAsList(mediaContent.tags);
                        mediaContent.poolsIds = ShouldHelper.treatAsList(mediaContent.poolsIds);
                        mediaContent.submediasIds = ShouldHelper.treatAsList(mediaContent.submediasIds);
                    });
                    ShouldHelper.deepEqualList(shouldResponseContent, responseContent.items);
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'mediaAddDeletePool', function () {
            it('[' + serie + '] ' + 'SUCCESS: mediaAddDeletePool', function (done) {
                var content = {
                    id: DataIds.MEDIA_1_ID,
                    deletePools: [DataIds.POOL_1_ID],
                    addPools: [DataIds.POOL_3_ID]
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaAddDeletePool', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'media/mediaGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.media.poolsIds.items).be.eql(content.addPools);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'SUCCESS: mediaAddDeletePool add only', function (done) {
                var content = {
                    id: DataIds.MEDIA_1_ID,
                    deletePools: [],
                    addPools: [DataIds.POOL_3_ID]
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaAddDeletePool', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'media/mediaGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.media.poolsIds.items).be.eql([DataIds.POOL_1_ID, DataIds.POOL_3_ID]);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'SUCCESS: mediaAddDeletePool delete only', function (done) {
                var content = {
                    id: DataIds.MEDIA_1_ID,
                    deletePools: [DataIds.POOL_1_ID],
                    addPools: []
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaAddDeletePool', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        /*
                         * Once a media was deleted from a pool that belongs to a tenant it is no longer accessible unless it belongs to a different
                         * pool where this user has access to.
                         * A media with all pools removed is not accessible for anyone because filtering by tenantId is not possible.
                         */
                        global.wsHelper.apiSecureFail(serie, 'media/mediaGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_CONSTRAINTS when add a non existing pool: media/mediaAddDeletePool ', function (done) {
                var content = {
                  id: DataIds.MEDIA_1_ID,
                  addPools: [DataIds.POOL_TEST_ID]
                };
                global.wsHelper.apiSecureFail(serie, 'media/mediaAddDeletePool', ProfileData.CLIENT_INFO_1, content, 'ERR_CONSTRAINTS', done);
            });
        });

        describe('[' + serie + '] ' + 'mediaUpdateProfilePicture', function () {
            it('[' + serie + '] ' + 'SUCCESS update/delete', function (done) {
                this.timeout(30000);
                var content = {
                    userId: DataIds.LOCAL_USER_ID,
                    action: 'update',
                    data: path.join(__dirname, '..', 'resources/test.jpg').replace(/\\/g, '/')
                };
                global.wsHelper.apiSecureSucc(serie, 'media/mediaUpdateProfilePicture', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setTimeout(function () {
                        content = {
                            userId: DataIds.LOCAL_USER_ID,
                            action: 'delete'
                        };
                        global.wsHelper.apiSecureSucc(serie, 'media/mediaUpdateProfilePicture', ProfileData.CLIENT_INFO_1, content, null, done);
                    }, 5000);
                });
            });
        });
    });

});
