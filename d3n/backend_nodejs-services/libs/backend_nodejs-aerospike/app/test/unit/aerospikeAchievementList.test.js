var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var AchievementData = require('./../config/achievement.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeAchievementList = KeyvalueService.Models.AerospikeAchievementList;

describe('AchievementList', function () {
    describe('AchievementList AS check', function () {
        it('HAVE #publish', function () {
            AerospikeAchievementList.should.be.have.property('publish');
            AerospikeAchievementList.publish.should.be.a.Function;
        });
        it('HAVE #unpublish', function () {
            AerospikeAchievementList.should.be.have.property('unpublish');
            AerospikeAchievementList.unpublish.should.be.a.Function;
        });
        it('HAVE #findOne', function () {
            AerospikeAchievementList.should.be.have.property('findOne');
            AerospikeAchievementList.findOne.should.be.a.Function;
        });
        it('HAVE #toJSON', function () {
            AerospikeAchievementList.should.be.have.property('toJSON');
            AerospikeAchievementList.toJSON.should.be.a.Function;
        });
        it('HAVE prototype #copy', function () {
            AerospikeAchievementList.prototype.should.be.have.property('copy');
            AerospikeAchievementList.prototype.copy.should.be.a.Function;
        });
        it('HAVE prototype #save', function () {
            AerospikeAchievementList.prototype.should.be.have.property('save');
            AerospikeAchievementList.prototype.save.should.be.a.Function;
        });
        it('HAVE prototype #remove', function () {
            AerospikeAchievementList.prototype.should.be.have.property('remove');
            AerospikeAchievementList.prototype.remove.should.be.a.Function;
        });
    });

    describe('AchievementList AS #publish', function () {
        it('test publish list OK', function (done) {
            async.series([
                function (next) {
                    return AerospikeAchievementList.publish(AchievementData.ACHIEVEMENT_1, next);
                },
                function (next) {
                    return AerospikeAchievementList.publish(AchievementData.ACHIEVEMENT_2, next);
                },
                function (next) {
                    return AerospikeAchievementList.unpublish(AchievementData.ACHIEVEMENT_1, next);
                },
                function (next) {
                    return AerospikeAchievementList.unpublish(AchievementData.ACHIEVEMENT_2, next);
                },
            ], done);
        });
    });
});
