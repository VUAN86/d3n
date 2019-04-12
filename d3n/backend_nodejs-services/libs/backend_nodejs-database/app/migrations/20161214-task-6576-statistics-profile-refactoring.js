// Statistics for profile
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.renameColumn(
            'profile',
            'questionsCreated',
            'stat_questionsCreated'
        ).then(function () {
            return queryInterface.renameColumn(
                'profile',
                'questionsReviewed',
                'stat_questionsReviewed'
            );
        }).then(function () {
            return queryInterface.renameColumn(
                'profile',
                'questionTranslationsCreated',
                'stat_questionsTranslationsCreated'
            );
        }).then(function () {
            return queryInterface.renameColumn(
                'profile',
                'questionTranslationsReviewed',
                'stat_questionsTranslationsReviewed'
            );
        }).then(function () {
            return queryInterface.renameColumn(
                'profile',
                'questionQuality',
                'stat_questionsQuality'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'gamesPlayed'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'moneyPlayed'
            );
        }).then(function () {
            return queryInterface.renameColumn(
                'profile',
                'favoriteGame',
                'stat_favoriteGame'
            );
        }).then(function () {
            return queryInterface.renameColumn(
                'profile',
                'issuesReported',
                'stat_issuesReported'
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.renameColumn(
            'profile',
            'stat_questionsCreated',
            'questionsCreated'
        ).then(function () {
            return queryInterface.renameColumn(
                'profile',
                'stat_questionsReviewed',
                'questionsReviewed'
            );
        }).then(function () {
            return queryInterface.renameColumn(
                'profile',
                'stat_questionsTranslationsCreated',
                'questionTranslationsCreated'
            );
        }).then(function () {
            return queryInterface.renameColumn(
                'profile',
                'stat_questionsTranslationsReviewed',
                'questionTranslationsReviewed'
            );
        }).then(function () {
            return queryInterface.renameColumn(
                'profile',
                'stat_questionsQuality',
                'questionQuality'
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'gamesPlayed',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'moneyPlayed',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.renameColumn(
                'profile',
                'stat_favoriteGame',
                'favoriteGame'
            );
        }).then(function () {
            return queryInterface.renameColumn(
                'profile',
                'stat_issuesReported',
                'issuesReported'
            );
        });
    }
};