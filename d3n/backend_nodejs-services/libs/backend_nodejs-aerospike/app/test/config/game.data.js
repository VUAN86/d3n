module.exports = {
    GAME_1: {
        "id": 1,
        "appId": 1,
        "gameId": 1,
        "title": "Game one title",
        "description": "Game one description",
        "isDeployed": 0,
        "gameModuleId": 1,
        "gameType": "duel",
        "status": "active",
        "gameTypeConfiguration": {
            "gameDuel": {
                "duelType": "normal",
                "playerInviteAccess": "friendsOnly",
                "timeToAcceptInvites": 10,
                "gameCancellationPriorGameStart": 10,
                "gameStartWarningMessage": 10,
                "emailNotification": 1,
                "playerGameReadiness": 10,
                "groupParing": "ranking",
                "groupSize": 10,
                "minimumPlayerNeeded": 2,
                "maximumPlayerAllowed": 100
            },
            "gameQuizBattle": {
                "liveTournamentId": 1,
                "lastGroupBehaviour": "cancelAndReject",
                "lastGroupAtQualifyEnd": "cancelAndReject",
                "startDate": "2016-11-28T20:48:51.000Z",
                "endDate": "2016-11-28T20:48:52.000Z"
            },
            "gameQuickQuiz": {
                "gameId": 1,
                "chance5050": 80,
                "chance5050BonusPoints": 5,
                "questionHint": 2,
                "questionHintBonusPoints": 7,
            }
        }
    }
}
