var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var BadgeData = require('./../config/achievement.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeBadgeList = KeyvalueService.Models.AerospikeBadgeList;

describe('BadgeList', function () {
    describe('BadgeList AS check', function () {
        it('HAVE #publish', function () {
            AerospikeBadgeList.should.be.have.property('publish');
            AerospikeBadgeList.publish.should.be.a.Function;
        });
        it('HAVE #unpublish', function () {
            AerospikeBadgeList.should.be.have.property('unpublish');
            AerospikeBadgeList.unpublish.should.be.a.Function;
        });
        it('HAVE #findOne', function () {
            AerospikeBadgeList.should.be.have.property('findOne');
            AerospikeBadgeList.findOne.should.be.a.Function;
        });
        it('HAVE #toJSON', function () {
            AerospikeBadgeList.should.be.have.property('toJSON');
            AerospikeBadgeList.toJSON.should.be.a.Function;
        });
        it('HAVE prototype #copy', function () {
            AerospikeBadgeList.prototype.should.be.have.property('copy');
            AerospikeBadgeList.prototype.copy.should.be.a.Function;
        });
        it('HAVE prototype #save', function () {
            AerospikeBadgeList.prototype.should.be.have.property('save');
            AerospikeBadgeList.prototype.save.should.be.a.Function;
        });
        it('HAVE prototype #remove', function () {
            AerospikeBadgeList.prototype.should.be.have.property('remove');
            AerospikeBadgeList.prototype.remove.should.be.a.Function;
        });
    });

    describe('BadgeList AS #publish', function () {
        it('test publish list OK', function (done) {
            async.series([
                function (next) {
                    return AerospikeBadgeList.publish(BadgeData.BADGE_1, next);
                },
                function (next) {
                    return AerospikeBadgeList.publish(BadgeData.BADGE_2, next);
                },
                function (next) {
                    return AerospikeBadgeList.unpublish(BadgeData.BADGE_1, next);
                },
                function (next) {
                    return AerospikeBadgeList.unpublish(BadgeData.BADGE_2, next);
                },
            ], done);
        });
    });
});
