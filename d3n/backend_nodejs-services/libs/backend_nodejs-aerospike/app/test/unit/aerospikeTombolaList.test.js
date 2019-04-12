var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var TombolaData = require('./../config/tombola.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeTombolaList = KeyvalueService.Models.AerospikeTombolaList;

describe('TombolaList', function () {
    describe('TombolaList AS check', function () {
        it('HAVE #publish', function () {
            AerospikeTombolaList.should.be.have.property('publish');
            AerospikeTombolaList.publish.should.be.a.Function;
        });
        it('HAVE #unpublish', function () {
            AerospikeTombolaList.should.be.have.property('unpublish');
            AerospikeTombolaList.unpublish.should.be.a.Function;
        });
        it('HAVE #findOne', function () {
            AerospikeTombolaList.should.be.have.property('findOne');
            AerospikeTombolaList.findOne.should.be.a.Function;
        });
        it('HAVE #toJSON', function () {
            AerospikeTombolaList.should.be.have.property('toJSON');
            AerospikeTombolaList.toJSON.should.be.a.Function;
        });
        it('HAVE prototype #copy', function () {
            AerospikeTombolaList.prototype.should.be.have.property('copy');
            AerospikeTombolaList.prototype.copy.should.be.a.Function;
        });
        it('HAVE prototype #save', function () {
            AerospikeTombolaList.prototype.should.be.have.property('save');
            AerospikeTombolaList.prototype.save.should.be.a.Function;
        });
        it('HAVE prototype #remove', function () {
            AerospikeTombolaList.prototype.should.be.have.property('remove');
            AerospikeTombolaList.prototype.remove.should.be.a.Function;
        });
    });

    describe('TombolaList AS #publish', function () {
        it('test publish list OK', function (done) {
            async.series([
                function (next) {
                    return AerospikeTombolaList.publish(TombolaData.TOMBOLA_1, next);
                },
                function (next) {
                    return AerospikeTombolaList.publish(TombolaData.TOMBOLA_2, next);
                },
                function (next) {
                    var tombolaUpdatedApps = _.cloneDeep(TombolaData.TOMBOLA_2);
                    tombolaUpdatedApps.applicationsIds = ['2', '4'];
                    return AerospikeTombolaList.publish(tombolaUpdatedApps, next);
                },
                function (next) {
                    return AerospikeTombolaList.unpublish(TombolaData.TOMBOLA_1, next);
                },
                function (next) {
                    return AerospikeTombolaList.unpublish(TombolaData.TOMBOLA_2, next);
                },
            ], done);
        });
    });
});
