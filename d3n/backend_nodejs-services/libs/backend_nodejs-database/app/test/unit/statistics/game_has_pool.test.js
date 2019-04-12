var should = require('should');
var Util = require('./../../util/statistics.js');
var DataIds = require('./../../config/_id.data.js');
var Game = Util.RdbmsModels.Game.Game;
var GameHasPool = Util.RdbmsModels.Game.GameHasPool;
var Question = Util.RdbmsModels.Question.Question;

describe('GAME_HAS_POOL', function () {
    it('create bulk', function (done) {
        GameHasPool.bulkCreate([
            { gameId: DataIds.GAME_1_ID, poolId: DataIds.POOL_2_ID },
            { gameId: DataIds.GAME_2_ID, poolId: DataIds.POOL_1_ID }
        ]).then(function () {
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Game.findOne({ where: {
                id: DataIds.GAME_1_ID
            }});
        }).then(function (game) {
            game.stat_poolsCount.should.equal(2);
        }).then(function() { 
            return Question.findOne({ where: {
                id: DataIds.QUESTION_1_ID
            }});
        }).then(function (question) {
            question.stat_associatedGames.should.equal(2);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('create single instance', function (done) {
        GameHasPool.create({
            gameId: DataIds.GAME_2_ID, poolId: DataIds.POOL_1_ID
        }).then(function () {
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Game.findOne({ where: {
                id: DataIds.GAME_2_ID
            }});
        }).then(function (game) {
            game.stat_poolsCount.should.equal(2);
        }).then(function() { 
            return Question.findOne({ where: {
                id: DataIds.QUESTION_1_ID
            }});
        }).then(function (question) {
            question.stat_associatedGames.should.equal(2);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('delete single instance', function (done) {
        GameHasPool.create({
            gameId: DataIds.GAME_2_ID, poolId: DataIds.POOL_1_ID 
        }).then(function() { 
            return GameHasPool.findOne({ where: {
                gameId: DataIds.GAME_1_ID, poolId: DataIds.POOL_1_ID
            }});
        }).then(function (gamePool) {
            return gamePool.destroy();
        }).then(function () {
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Game.findOne({ where: {
                id: DataIds.GAME_1_ID
            }});
        }).then(function (game) {
            game.stat_poolsCount.should.equal(0);
        }).then(function() { 
            return Question.findOne({ where: {
                id: DataIds.QUESTION_1_ID
            }});
        }).then(function (question) {
            question.stat_associatedGames.should.equal(1);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('delete bulk', function (done) {
        GameHasPool.bulkCreate([
            { gameId: DataIds.GAME_1_ID, poolId: DataIds.POOL_2_ID },
            { gameId: DataIds.GAME_2_ID, poolId: DataIds.POOL_1_ID }
        ]).then(function () {
            return GameHasPool.destroy({
                where: { gameId: DataIds.GAME_1_ID },
                individualHooks: true
            });
        }).then(function () {
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Game.findOne({ where: {
                id: DataIds.GAME_1_ID
            }});
        }).then(function (game) {
            game.stat_poolsCount.should.equal(0);
        }).then(function() { 
            return Question.findOne({ where: {
                id: DataIds.QUESTION_1_ID
            }});
        }).then(function (question) {
            question.stat_associatedGames.should.equal(1);
            done();
        }).catch(function(err) {
            done(err);
        });
    });
});
