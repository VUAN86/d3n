var _ = require('lodash'),
    Config = require('./../../config/config.js'),
    Errors = require('./../../config/errors.js'),
    DataIds = require('./_id.data.js'),
    Database = require('./../../../index.js').getInstance(Config),
    DatabaseErrors = Database.Errors,
    RdbmsService = Database.RdbmsService,
    Media = RdbmsService.Models.Media.Media,
    MediaHasTag = RdbmsService.Models.Media.MediaHasTag;

module.exports = {
    MEDIA_1: {
        $id: DataIds.MEDIA_1_ID,
        namespace: Media.constants().NAMESPACE_QUESTION,
        metaData: '{type:"image/png"}',
        contentType: 'image',
        type: 'original',
        validFromDate: null,
        validToDate: null
    },
    MEDIA_2: {
        $id: DataIds.MEDIA_2_ID,
        namespace: Media.constants().NAMESPACE_QUESTION,
        metaData: '{}',
        contentType: 'video',
        type: 'original',
        validFromDate: null,
        validToDate: null
    },
    MEDIA_TEST: {
        $id: DataIds.MEDIA_TEST_ID,
        namespace: Media.constants().NAMESPACE_QUESTION,
        metaData: '{}',
        contentType: 'audio',
        type: 'original',
        validFromDate: _.now(),
        validToDate: _.now() + 1000
    },

    load: function (testSet) {
        return testSet
            .createSeries(Media, [this.MEDIA_1, this.MEDIA_2])
    },

};
