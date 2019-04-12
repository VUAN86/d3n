var Config = require('./../../config/config.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var RdbmsService = Database.RdbmsService;
var Media = RdbmsService.Models.Media.Media;

module.exports = {
    MEDIA_1: {
        id: DataIds.MEDIA_1_ID,
        namespace: Media.constants().NAMESPACE_QUESTION,
        metaData: '{type:"image/png"}',
        contentType: 'image',
        validFromDate: null,
        validToDate: null,
        type: 'original'
    },
    MEDIA_2: {
        id: DataIds.MEDIA_2_ID,
        namespace: Media.constants().NAMESPACE_QUESTION,
        metaData: '{}',
        contentType: 'video',
        validFromDate: null,
        validToDate: null,
        type: 'original'
    },
    MEDIA_TEST: {
        id: DataIds.MEDIA_TEST_ID,
        namespace: Media.constants().NAMESPACE_QUESTION,
        metaData: '{}',
        contentType: 'audio',
        validFromDate: DateUtils.isoNow(),
        validToDate: DateUtils.isoFuture()
    },

    clean: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(Media, [self.MEDIA_1, self.MEDIA_2, self.MEDIA_TEST])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    load: function (done) {
        var self = this;
        RdbmsService.load()
            .createSeries(Media, [self.MEDIA_1, self.MEDIA_2])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    }
};
