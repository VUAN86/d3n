var _ = require('lodash');
var Errors = require('./../config/errors.js');
var Config = require('./../config/config.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var ApplicationApiFactory = require('./../factories/applicationApiFactory.js');
var Database = require('nodejs-database').getInstance(Config);
var DummyApiFactory = require('../factories/dummyApiFactory.js');

module.exports = {
    
    appReferralLink: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            DummyApiFactory.appReferralLink(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
    
    /**
     * WS dummy/getDashboard
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    getDashboard: function (message, clientSession) {
        var data = {
                "QUIZ24": {
                    "numberOfGamesAvailable": 5, // number of games available for this player
                "lastPlayedGame": {
                    "name": "Quiz Allgemeinwissen", // game name in the language of the user or default
                    "date": "2017-02-14T12:00:05Z"
                },
                "mostPlayedGame": { // most played game by all players in the app
                    "name": "Quiz TV & Movie", // game name in the language of the user or default
                    "numberOfPlayers": 1337 // number of unique players played this game
                }
            },
            "DUEL": { // mgi in gameIds?
                "nextInvitation": {
                    "gameName": "Top Game", // game name in the language of the user or default
                    "gameId": 43,
                    "expirationDate": "2017-02-24T13:00:05Z", // invitation date
                    "userId": "asdhasdhajksghd-asdkasd-asdasd",
                    "userNickname": "TopQuizzer"
                },
                "nextPublicGame": {
                    "gameName": "Quiz Superduel", // game name in the language of the user or default
                        "gameId": 43,
                        "expirationDate": "2017-02-16T14:00:05Z", // invitation date
                        "creatorUserId": "asdhasdhajksghd-asdkasd-asdasd",
                        "creatorUserNickname": "Moderator"
                },
                "lastDuel": {
                    "gameName": "Quiz Sport", // game name in the language of the user or default
                        "gameId": 43,
                        "date": "2017-02-13T15:00:05Z", // invitation date
                        "opponentUserId": "asdhasdhajksghd-asdkasd-asdasd",
                        "opponentUserNickname": "Dr.Strangelove"
                },
                "numberOfInvitations": 2,
                "numberOfPublicGames": 5
            },
            "TOURNAMENT": { // mgi in gameIds
                "nextInvitation": {
                    "gameName": "Deutschlands bester Quizmaster", // game name in the language of the user or default
                    "gameId": 43,
                    "expirationDate": "2017-02-16T14:00:05Z", // invitation date
                    "userId": "asdhasdhajksghd-asdkasd-asdasd",
                    "userNickname": "Super Quizmaster"
                },
                "nextTournament": {
                    "gameName": "Das große GOT Tournament", // game name in the language of the user or default
                    "gameId": 43,
                    "expirationDate": "2017-02-16T14:00:05Z", // invitation date
                },
                "numberOfInvitations": 2,
                "numberOfTournaments": 5
            },
            "USER_TOURNAMENT": {
                "nextInvitation": {
                    "gameName": "Barbattle", // game name in the language of the user or default
                    "gameId": 43,
                    "expirationDate": "2017-02-16T14:00:05Z", // invitation date
                    "userId": "asdhasdhajksghd-asdkasd-asdasd",
                    "userNickname": "H34dHunt3r"
                },
                "nextTournament": {
                    "gameName": "Quiz Verein 23", // game name in the language of the user or default
                    "gameId": 43,
                    "expirationDate": "2017-02-16T14:00:05Z", // invitation date
                    "creatorUserId": "asdhasdhajksghd-asdkasd-asdasd",
                    "creatorUserNickname": "Lord Helmchen"
                },
                "lastTournament": {
                    "gameName": "Tournament 1000", // game name in the language of the user or default
                    "gameId": 43,
                    "date": "2017-02-16T14:00:05Z", // invitation date
                    "opponentsNumber": 12,
                    "placement": 12
                },
                "numberOfInvitations": 2,
                "numberOfTournaments": 5
            },
            "QUIZ_BATTLE": {
                "nextInvitation": {
                    "gameName": "Battlerattle", // game name in the language of the user or default
                    "gameId": 43,
                    "expirationDate": "2017-02-16T14:00:05Z", // invitation date
                    "userId": "asdhasdhajksghd-asdkasd-asdasd",
                    "userNickname": "Lord Helmchen"
                },
                "nextQuizBattle": {
                    "gameName": "Das große Quizgame", // game name in the language of the user or default
                    "gameId": 43,
                    "expirationDate": "2017-02-16T14:00:05Z", // invitation date
                    "creatorUserId": "asdhasdhajksghd-asdkasd-asdasd",
                    "creatorUserNickname": "Blümchen"
                },
                "numberOfQuizBattles": 5
            }
        };
        CrudHelper.handleProcessed(null, data, message, clientSession);
    },

    /**
     * WS dummy/ping
     * Returns pingResponse immediately
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    ping: function (message, clientSession) {
        CrudHelper.handleProcessed(null, {}, message, clientSession);
    },

    playerGameHistory: function(message, clientSession) {
        var data = {
            items: [
                {
                    "gameId": 1,
                    "applicationId": 1,
                    "date": "2017-02-05T23:59:00T",
                    "adsViewed": 1,
                    "moneyWon": 0,
                    "creditWon": 1,
                    "bonuspointWon": 200,
                    "wonVoucherId": 1,
                    "userWinningComponentId": null,
                    "gameEntryAmount": 0,
                    "gameEntryCurrency": "BONUS",
                    "status": "FINISHED"
                },
                {
                    "gameId": 2,
                    "applicationId": 1,
                    "date": "2017-02-07T15:20:10T",
                    "adsViewed": 0,
                    "moneyWon": 0,
                    "creditWon": 0,
                    "bonuspointWon": 0,
                    "wonVoucherId": null,
                    "userWinningComponentId": null,
                    "gameEntryAmount": 0,
                    "gameEntryCurrency": "BONUS",
                    "status": "CANCELLED"
                },
                {
                    "gameId": 5,
                    "applicationId": 1,
                    "date": "2017-02-13T17:23:11T",
                    "adsViewed": 2,
                    "moneyWon": 0,
                    "creditWon": 0,
                    "bonuspointWon": 0,
                    "wonVoucherId": null,
                    "userWinningComponentId": null,
                    "gameEntryAmount": 2,
                    "gameEntryCurrency": "MONEY",
                    "status": "IN_PROGRESS"
                },
                {
                    "gameId": 7,
                    "applicationId": 1,
                    "date": "2017-02-21T13:29:03T",
                    "adsViewed": 3,
                    "moneyWon": 1,
                    "creditWon": 0,
                    "bonuspointWon": 200,
                    "wonVoucherId": null,
                    "userWinningComponentId": null,
                    "gameEntryAmount": 1,
                    "gameEntryCurrency": "CREDIT",
                    "status": "WAITING_FOR_OPPONENT"
                }
            ],
            limit: 20,
            total: 4,
            offset: 0
        };
        CrudHelper.handleProcessed(null, data, message, clientSession);
    }
};