var _ = require('lodash');

module.exports = {

    questionMediaList: function (content, callback) {
        try {
            var medias = [];
            _.forEach(content, function(step) {
                if (step.medias) {
                    medias = _.concat(medias, step.medias);
                }
            });
            return setImmediate(callback, null, medias);
        } catch (ex) {
            return setImmediate(callback, ex);
        }

    },

    questionStepsList: function (content, callback) {
        try {
            return setImmediate(callback, null, content);
        } catch (ex) {
            return setImmediate(callback, ex);
        }

    },
};
