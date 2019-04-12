var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var Media = RdbmsService.Models.Media.Media;
var Workorder = RdbmsService.Models.Workorder.Workorder;
var MediaHasTag = RdbmsService.Models.Media.MediaHasTag;
var Tag = RdbmsService.Models.Question.Tag;

module.exports = {
    MEDIA_1: {
        id: DataIds.MEDIA_1_ID,
        parentId: null,
        namespace: Media.constants().NAMESPACE_APP,
        workorderId: DataIds.WORKORDER_1_ID,
        metaData: {},
        contentType: 'video',
        type: 'original',
        title: 'dev4m first video',
        description: 'First video uploaded by a developer',
        source: 'Video received via email',
        copyright: 'GPL - General Public Licence',
        licence: 'Licence A32HDE',
        usageInformation: 'This video can be used in all conditions',
        validFromDate: null,
        validToDate: null,
        originalFilename: 'pool.jpg',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        tags: [DataIds.TAG_1_TAG],
        poolsIds: [DataIds.POOL_1_ID],
        submediasIds: [DataIds.MEDIA_2_ID]
    },
    MEDIA_2: {
        id: DataIds.MEDIA_2_ID,
        parentId: DataIds.MEDIA_1_ID,
        namespace: Media.constants().NAMESPACE_QUESTION,
        workorderId: DataIds.WORKORDER_1_ID,
        metaData: { 'type': 'img/jpg', 'width': 100, 'height': 200 },
        contentType: 'image',
        type: 'mask',
        title: 'Second media, an image',
        description: 'Second media uploaded by developer, an image',
        source: 'Image from developer',
        copyright: 'MIT',
        licence: 'Licence HED12',
        usageInformation: 'This image can be used everywhere',
        validFromDate: null,
        validToDate: null,
        originalFilename: 'sun.png',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        tags: [DataIds.TAG_1_TAG],
        poolsIds: [DataIds.POOL_1_ID],
        submediasIds: []
    },
    MEDIA_3: {
        id: DataIds.MEDIA_3_ID,
        parentId: null,
        namespace: Media.constants().NAMESPACE_QUESTION,
        workorderId: DataIds.WORKORDER_2_ID,
        metaData: { 'type': 'img/jpg', 'width': 100, 'height': 200 },
        contentType: 'image',
        type: 'mask',
        title: 'Third media, an image',
        description: 'Third media uploaded by developer, an image',
        source: 'Image from developer',
        copyright: 'MIT',
        licence: 'Licence HED12',
        usageInformation: 'This image can be used everywhere',
        validFromDate: null,
        validToDate: null,
        originalFilename: 'sun.png',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        tags: [DataIds.TAG_2_TAG],
        poolsIds: [DataIds.POOL_2_ID],
        submediasIds: [DataIds.MEDIA_4_ID]
    },
    MEDIA_4: {
        id: DataIds.MEDIA_4_ID,
        parentId: DataIds.MEDIA_3_ID,
        namespace: Media.constants().NAMESPACE_QUESTION,
        workorderId: DataIds.WORKORDER_2_ID,
        metaData: { 'type': 'img/jpg', 'width': 100, 'height': 200 },
        contentType: 'image',
        type: 'mask',
        title: 'Third media, an image',
        description: 'Third media uploaded by developer, an image',
        source: 'Image from developer',
        copyright: 'MIT',
        licence: 'Licence HED12',
        usageInformation: 'This image can be used everywhere',
        validFromDate: null,
        validToDate: null,
        originalFilename: 'sun.png',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        tags: [DataIds.TAG_2_TAG],
        poolsIds: [DataIds.POOL_2_ID],
        submediasIds: []
    },    
    MEDIA_NO_DEPENDENCIES: {
        id: DataIds.MEDIA_NO_DEPENDENCIES_ID,
        parentId: null,
        namespace: Media.constants().NAMESPACE_APP,
        workorderId: null,
        metaData: { 'type': 'img/png', 'width': 400, 'height': 800 },
        contentType: 'image',
        type: 'original',
        title: 'Media with no dependencies',
        description: 'This media has no dependencies or tags',
        source: 'Image that has no dependencies',
        copyright: 'Public Domain',
        licence: 'Licensed for lifetime',
        usageInformation: 'The image can be used only in games',
        validFromDate: null,
        validToDate: null,
        originalFilename: 'sun.png',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow()
    },
    MEDIA_TEST: {
        id: DataIds.MEDIA_TEST_ID,
        parentId: null,
        workorderId: null,
        namespace: Media.constants().NAMESPACE_APP,
        metaData: {},
        contentType: 'image',
        type: 'original',
        title: 'Test media',
        description: 'Test media that is created only during tests',
        source: '',
        copyright: '',
        licence: '',
        usageInformation: '',
        originalFilename: 'sun.png',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        tags: [],
        poolsIds: [],
        submediasIds: []
    },

    TAG_1: {
        tag: DataIds.TAG_1_TAG,
        isApproved: 1,
        approvedDate: DateUtils.isoNow(),
        tenantId: DataIds.TENANT_1_ID
    },

    TAG_2: {
        tag: DataIds.TAG_2_TAG,
        isApproved: 0,
        approvedDate: null,
        tenantId: DataIds.TENANT_2_ID
    },

    TAG_TEST: {
        tag: DataIds.TAG_TEST_TAG,
        isApproved: 0,
        approvedDate: null
    },

    MEDIA_1_HAS_TAG_1: {
        mediaId: DataIds.MEDIA_1_ID,
        tagTag: DataIds.TAG_1_TAG
    },

    MEDIA_1_HAS_TAG_2: {
        mediaId: DataIds.MEDIA_1_ID,
        tagTag: DataIds.TAG_2_TAG
    },

    MEDIA_2_HAS_TAG_1: {
        mediaId: DataIds.MEDIA_2_ID,
        tagTag: DataIds.TAG_1_TAG
    },

    MEDIA_2_HAS_TAG_2: {
        mediaId: DataIds.MEDIA_2_ID,
        tagTag: DataIds.TAG_2_TAG
    },

    cleanManyToMany: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(MediaHasTag, [self.MEDIA_1_HAS_TAG_1, self.MEDIA_1_HAS_TAG_2, self.MEDIA_2_HAS_TAG_1, self.MEDIA_2_HAS_TAG_2], 'mediaId')
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    loadManyToMany: function (done) {
        var self = this;
        RdbmsService.load()
            .createSeries(MediaHasTag, [self.MEDIA_1_HAS_TAG_1, self.MEDIA_1_HAS_TAG_2, self.MEDIA_2_HAS_TAG_1, self.MEDIA_2_HAS_TAG_2], 'mediaId')
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    cleanEntities: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(Media, [self.MEDIA_1, self.MEDIA_2, self.MEDIA_3, self.MEDIA_4, self.MEDIA_NO_DEPENDENCIES, self.MEDIA_TEST])
            .removeSeries(Tag, [self.TAG_1, self.TAG_2, self.TAG_TEST], 'tag')
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });

    },

    loadEntities: function (done) {
        var self = this;
        RdbmsService.load()
            .createSeries(Media, [self.MEDIA_1, self.MEDIA_2, self.MEDIA_3, self.MEDIA_4, self.MEDIA_NO_DEPENDENCIES])
            .createSeries(Tag, [self.TAG_1, self.TAG_2])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    }
};
