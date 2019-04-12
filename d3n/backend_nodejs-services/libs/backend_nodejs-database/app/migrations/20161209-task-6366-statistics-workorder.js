// Adjust field Game.gameEntryCurrency enum
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'workorder',
            'stat_questionsRequested',
            {
                type: Sequelize.INTEGER(11),
                allowNull: false,
                defaultValue: 0
            }
        ).then(function () {
            return queryInterface.addColumn(
                'workorder',
                'stat_questionsCompleted',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'workorder',
                'stat_questionsInProgress',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'workorder',
                'stat_progressPercentage',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'workorder',
                'stat_questionsInTranslation',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'workorder',
                'stat_questionTranslationsCompleted',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'workorder',
                'stat_questionTranslationsInProgress',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'workorder',
                'stat_translationsProgressPercentage',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'workorder',
                'stat_questionsPublished',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'workorder',
                'stat_translationsPublished',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'workorder',
                'stat_questionsComplexityLevel1',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'workorder',
                'stat_questionsComplexityLevel2',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'workorder',
                'stat_questionsComplexityLevel3',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'workorder',
                'stat_questionsComplexityLevel4',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'workorder',
                'stat_numberOfLanguages',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'workorder',
            'stat_questionsRequested'
        ).then(function () {
            return queryInterface.removeColumn(
                'workorder',
                'stat_questionsCompleted'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'workorder',
                'stat_questionsInProgress'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'workorder',
                'stat_progressPercentage'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'workorder',
                'stat_questionsInTranslation'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'workorder',
                'stat_questionTranslationsCompleted'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'workorder',
                'stat_questionTranslationsInProgress'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'workorder',
                'stat_translationsProgressPercentage'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'workorder',
                'stat_questionsPublished'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'workorder',
                'stat_translationsPublished'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'workorder',
                'stat_questionsComplexityLevel1'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'workorder',
                'stat_questionsComplexityLevel2'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'workorder',
                'stat_questionsComplexityLevel3'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'workorder',
                'stat_questionsComplexityLevel4'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'workorder',
                'stat_numberOfLanguages'
            );
        });
    }
};