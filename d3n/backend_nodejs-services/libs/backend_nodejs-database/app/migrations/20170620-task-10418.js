// remove profile stats column
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn('profile', 'stat_handicap')
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_appsInstalled');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_friendsInvited');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_friendsBlocked');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_gamesPlayed');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_gamesInvitedFromFriends');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_gamesFriendsInvitedToo');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_gamesPlayedWithFriends');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_gamesPlayedWithPublic');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_gamesWon');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_gamesLost');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_gamesDrawn');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_rightAnswers');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_wrongAnswers');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_averageAnswerSpeed');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_skippedQuestions');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_adsViewed');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_paidWinningComponentsPlayed');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_skippedWinningComponents');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_voucherWon');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_superPrizesWon');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_bonusPointsWon');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_creditsWon');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_moneyPlayed');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_moneyWon');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_totalCreditsPurchased');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_totalMoneyCharged');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_questionsCreated');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_questionsReviewed');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_questionsTranslationsCreated');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_questionsTranslationsReviewed');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_questionsQuality');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_favoriteGame');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_issuesReported');
        })
        .then(function () {
            queryInterface.removeColumn('profile', 'stat_freeWinningComponentsPlayed');
        });
    },
    
    down: function (queryInterface, Sequelize) {
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
};