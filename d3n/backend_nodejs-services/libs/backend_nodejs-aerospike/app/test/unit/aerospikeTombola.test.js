var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var TombolaData = require('./../config/tombola.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeTombola = KeyvalueService.Models.AerospikeTombola;

describe('Tombola', function () {
    describe('Tombola AS check', function () {
        it('HAVE #publish', function () {
            AerospikeTombola.should.be.have.property('publish');
            AerospikeTombola.publish.should.be.a.Function;
        });
        it('HAVE #unpublish', function () {
            AerospikeTombola.should.be.have.property('unpublish');
            AerospikeTombola.unpublish.should.be.a.Function;
        });
        it('HAVE #findOne', function () {
            AerospikeTombola.should.be.have.property('findOne');
            AerospikeTombola.findOne.should.be.a.Function;
        });
        it('HAVE #toJSON', function () {
            AerospikeTombola.should.be.have.property('toJSON');
            AerospikeTombola.toJSON.should.be.a.Function;
        });
        it('HAVE prototype #copy', function () {
            AerospikeTombola.prototype.should.be.have.property('copy');
            AerospikeTombola.prototype.copy.should.be.a.Function;
        });
        it('HAVE prototype #save', function () {
            AerospikeTombola.prototype.should.be.have.property('save');
            AerospikeTombola.prototype.save.should.be.a.Function;
        });
        it('HAVE prototype #remove', function () {
            AerospikeTombola.prototype.should.be.have.property('remove');
            AerospikeTombola.prototype.remove.should.be.a.Function;
        });
    });

    describe('Tombola AS #publish', function () {
        it('test publish OK', function (done) {
            AerospikeTombola.publish(TombolaData.TOMBOLA_1, function (err, app) {
                AerospikeTombola.unpublish(TombolaData.TOMBOLA_1, function (err, app) {
                    if (err) return done(err);
                    done();
                });
            });
        });
    });
});
