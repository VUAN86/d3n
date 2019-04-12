var should = require('should');
var Util = require('./../../util/statistics.js');
var DataIds = require('./../../config/_id.data.js');
var Application = Util.RdbmsModels.Application.Application;
var ApplicationHasGame = Util.RdbmsModels.Application.ApplicationHasGame;

describe('APPLICATION_HAS_GAME', function () {
    it('create bulk', function (done) {
        ApplicationHasGame.bulkCreate([
            { applicationId: DataIds.APPLICATION_2_ID, gameId: DataIds.GAME_1_ID },
            { applicationId: DataIds.APPLICATION_2_ID, gameId: DataIds.GAME_2_ID }
        ]).then(function () {
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Application.findOne({ where: {
                id: DataIds.APPLICATION_2_ID
            }});
        }).then(function (application) {
            application.stat_games.should.equal(2);
            application.stat_quizGames.should.equal(2);
            application.stat_moneyGames.should.equal(1);
            application.stat_bettingGames.should.equal(0);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('create single instance', function (done) {
        ApplicationHasGame.create({
            applicationId: DataIds.APPLICATION_2_ID, gameId: DataIds.GAME_1_ID 
        }).then(function () {
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Application.findOne({ where: {
                id: DataIds.APPLICATION_2_ID
            }});
        }).then(function (application) {
            application.stat_games.should.equal(1);
            application.stat_quizGames.should.equal(1);
            application.stat_moneyGames.should.equal(1);
            application.stat_bettingGames.should.equal(0);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('delete single instance', function (done) {
        ApplicationHasGame.create({
            applicationId: DataIds.APPLICATION_2_ID, gameId: DataIds.GAME_1_ID 
        }).then(function() { 
            return ApplicationHasGame.findOne({ where: {
                applicationId: DataIds.APPLICATION_2_ID, gameId: DataIds.GAME_1_ID 
            }});
        }).then(function (applicationGame) {
            return applicationGame.destroy();
        }).then(function () {
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Application.findOne({ where: {
                id: DataIds.APPLICATION_2_ID
            }});
        }).then(function (application) {
            application.stat_games.should.equal(0);
            application.stat_quizGames.should.equal(0);
            application.stat_moneyGames.should.equal(0);
            application.stat_bettingGames.should.equal(0);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('delete bulk', function (done) {
        ApplicationHasGame.bulkCreate([
            { applicationId: DataIds.APPLICATION_2_ID, gameId: DataIds.GAME_1_ID },
            { applicationId: DataIds.APPLICATION_2_ID, gameId: DataIds.GAME_2_ID }
        ]).then(function() {
            return ApplicationHasGame.destroy({
                where: { applicationId: DataIds.APPLICATION_2_ID },
                individualHooks: true
            });
        }).then(function () {
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Application.findOne({ where: {
                id: DataIds.APPLICATION_2_ID
            }});
        }).then(function (application) {
            application.stat_games.should.equal(0);
            application.stat_quizGames.should.equal(0);
            application.stat_moneyGames.should.equal(0);
            application.stat_bettingGames.should.equal(0);
            done();
        }).catch(function(err) {
            done(err);
        });
    });
});
