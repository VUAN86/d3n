// Adjust field Game.gameEntryCurrency enum
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'pool',
            'stat_assignedWorkorders',
            {
                type: Sequelize.INTEGER(11),
                allowNull: false,
                defaultValue: 0
            }
        ).then(function () {
            return queryInterface.addColumn(
                'pool',
                'stat_assignedQuestions',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'pool',
                'stat_questionsComplexityLevel1',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'pool',
                'stat_questionsComplexityLevel2',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'pool',
                'stat_questionsComplexityLevel3',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'pool',
                'stat_questionsComplexityLevel4',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'pool',
                'stat_regionsSupported',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'pool',
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
            'pool',
            'stat_assignedWorkorders'
        ).then(function () {
            return queryInterface.removeColumn(
                'pool',
                'stat_assignedQuestions'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'pool',
                'stat_questionsComplexityLevel1'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'pool',
                'stat_questionsComplexityLevel2'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'pool',
                'stat_questionsComplexityLevel3'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'pool',
                'stat_questionsComplexityLevel4'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'pool',
                'stat_regionsSupported'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'pool',
                'stat_numberOfLanguages'
            );
        });
    }
};