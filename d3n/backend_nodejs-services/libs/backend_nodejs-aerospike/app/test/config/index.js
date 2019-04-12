var _ = require('lodash');
var logger = require('nodejs-logger')();
var ProfileData = require('./profile.data.js');
var SessionData = require('./session.data.js');

module.exports = {
    loadBefore: function (done) {
        SessionData.loadAS(function (err) {
            if (err) {
                logger.error('Aerospike session data error.');
                return done(err);
            }
            logger.info('Aerospike session data loaded.');
            return done();
        });
    },
    loadBeforeEach: function (done) {
        ProfileData.loadAS(function (err) {
            if (err) {
                logger.error('Aerospike profile data error.');
                return done(err);
            }
            logger.info('Aerospike profile data loaded.');
            return done();
        });
    }
}