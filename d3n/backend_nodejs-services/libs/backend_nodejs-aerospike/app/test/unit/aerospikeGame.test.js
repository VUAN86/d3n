var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var GameData = require('./../config/game.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeGame = KeyvalueService.Models.AerospikeGame;

describe('Game', function () {
    describe('Game AS check', function () {
        it('HAVE #findOne', function () {
            AerospikeGame.should.be.have.property('findOne');
            AerospikeGame.findOne.should.be.a.Function;
        });
        it('HAVE #toJSON', function () {
            AerospikeGame.should.be.have.property('toJSON');
            AerospikeGame.toJSON.should.be.a.Function;
        });
        it('HAVE prototype #copy', function () {
            AerospikeGame.prototype.should.be.have.property('copy');
            AerospikeGame.prototype.copy.should.be.a.Function;
        });
        it('HAVE prototype #save', function () {
            AerospikeGame.prototype.should.be.have.property('save');
            AerospikeGame.prototype.save.should.be.a.Function;
        });
        it('HAVE prototype #remove', function () {
            AerospikeGame.prototype.should.be.have.property('remove');
            AerospikeGame.prototype.remove.should.be.a.Function;
        });
    });

    describe('Game AS #remove', function () {
        it('test remove OK', function (done) {
            AerospikeGame.publishGame(GameData.GAME_1, function (err, game) {
                AerospikeGame.unpublishGame(GameData.GAME_1, function (err, game) {
                    if (err) return done(err);
                    done();
                });
            });
        });
    });
});
