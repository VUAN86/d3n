var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var BadgeData = require('./../config/achievement.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeBadge = KeyvalueService.Models.AerospikeBadge;

describe('Badge', function () {
    describe('Badge AS check', function () {
        it('HAVE #publish', function () {
            AerospikeBadge.should.be.have.property('publish');
            AerospikeBadge.publish.should.be.a.Function;
        });
        it('HAVE #unpublish', function () {
            AerospikeBadge.should.be.have.property('unpublish');
            AerospikeBadge.unpublish.should.be.a.Function;
        });
        it('HAVE #findOne', function () {
            AerospikeBadge.should.be.have.property('findOne');
            AerospikeBadge.findOne.should.be.a.Function;
        });
        it('HAVE #toJSON', function () {
            AerospikeBadge.should.be.have.property('toJSON');
            AerospikeBadge.toJSON.should.be.a.Function;
        });
        it('HAVE prototype #copy', function () {
            AerospikeBadge.prototype.should.be.have.property('copy');
            AerospikeBadge.prototype.copy.should.be.a.Function;
        });
        it('HAVE prototype #save', function () {
            AerospikeBadge.prototype.should.be.have.property('save');
            AerospikeBadge.prototype.save.should.be.a.Function;
        });
        it('HAVE prototype #remove', function () {
            AerospikeBadge.prototype.should.be.have.property('remove');
            AerospikeBadge.prototype.remove.should.be.a.Function;
        });
    });

    describe('Badge AS #publish', function () {
        it('test publish OK', function (done) {
            AerospikeBadge.publish(BadgeData.BADGE_1, function (err, app) {
                AerospikeBadge.unpublish(BadgeData.BADGE_1, function (err, app) {
                    if (err) return done(err);
                    done();
                });
            });
        });
    });
});
