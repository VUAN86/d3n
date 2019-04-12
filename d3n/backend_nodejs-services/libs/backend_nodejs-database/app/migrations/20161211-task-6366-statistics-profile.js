// Statistics for profile
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'profile',
            'stat_handicap',
            {
                type: Sequelize.INTEGER(11),
                allowNull: false,
                defaultValue: 0
            }
        ).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_appsInstalled',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_friendsInvited',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_friendsAccepted',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_friendsRejected',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_gamesPlayed',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_gamesInvitedFromFriends',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_gamesFriendsInvitedToo',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_gamesPlayedWithFriends',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_gamesPlayedWithPublic',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_gamesWon',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_gamesLost',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_gamesDrawn',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_rightAnswers',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_wrongAnswers',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_averageAnswerSpeed',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_skippedQuestions',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_adsViewed',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_paidWinningComponentsPlayed',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_freeWinningComponentsPlayed',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_skippedWinningComponents',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_voucherWon',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_superPrizesWon',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_bonusPointsWon',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_creditsWon',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_moneyWon',
                {
                    type: Sequelize.DECIMAL(10,2),
                    allowNull: false,
                    defaultValue: '0.00'
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_totalCreditsPurchased',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_totalMoneyCharged',
                {
                    type: Sequelize.DECIMAL(10,2),
                    allowNull: false,
                    defaultValue: '0.00'
                }
            );
        })
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'profile',
            'stat_handicap'
        ).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_appsInstalled'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_friendsInvited'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_friendsAccepted'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_friendsRejected'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_gamesPlayed'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_gamesInvitedFromFriends'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_gamesFriendsInvitedToo'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_gamesPlayedWithFriends'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_gamesPlayedWithPublic'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_gamesWon'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_gamesLost'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_gamesDrawn'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_rightAnswers'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_wrongAnswers'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_averageAnswerSpeed'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_skippedQuestions'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_adsViewed'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_paidWinningComponentsPlayed'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_freeWinningComponentsPlayed'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_skippedWinningComponents'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_voucherWon'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_superPrizesWon'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_bonusPointsWon'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_creditsWon'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_moneyWon'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_totalCreditsPurchased'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'stat_totalMoneyCharged'
            );
        });
    }
};