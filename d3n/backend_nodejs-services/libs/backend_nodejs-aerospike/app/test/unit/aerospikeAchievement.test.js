var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var AchievementData = require('./../config/achievement.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeAchievement = KeyvalueService.Models.AerospikeAchievement;

describe('Achievement', function () {
    describe('Achievement AS check', function () {
        it('HAVE #publish', function () {
            AerospikeAchievement.should.be.have.property('publish');
            AerospikeAchievement.publish.should.be.a.Function;
        });
        it('HAVE #unpublish', function () {
            AerospikeAchievement.should.be.have.property('unpublish');
            AerospikeAchievement.unpublish.should.be.a.Function;
        });
        it('HAVE #findOne', function () {
            AerospikeAchievement.should.be.have.property('findOne');
            AerospikeAchievement.findOne.should.be.a.Function;
        });
        it('HAVE #toJSON', function () {
            AerospikeAchievement.should.be.have.property('toJSON');
            AerospikeAchievement.toJSON.should.be.a.Function;
        });
        it('HAVE prototype #copy', function () {
            AerospikeAchievement.prototype.should.be.have.property('copy');
            AerospikeAchievement.prototype.copy.should.be.a.Function;
        });
        it('HAVE prototype #save', function () {
            AerospikeAchievement.prototype.should.be.have.property('save');
            AerospikeAchievement.prototype.save.should.be.a.Function;
        });
        it('HAVE prototype #remove', function () {
            AerospikeAchievement.prototype.should.be.have.property('remove');
            AerospikeAchievement.prototype.remove.should.be.a.Function;
        });
    });

    describe('Achievement AS #publish', function () {
        it('test publish OK', function (done) {
            AerospikeAchievement.publish(AchievementData.ACHIEVEMENT_1, function (err, app) {
                AerospikeAchievement.unpublish(AchievementData.ACHIEVEMENT_1, function (err, app) {
                    if (err) return done(err);
                    done();
                });
            });
        });
    });
});
