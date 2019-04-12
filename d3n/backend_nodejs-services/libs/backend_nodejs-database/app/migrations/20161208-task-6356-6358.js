// Adjust field Game.gameEntryCurrency enum
module.exports = {
    up: function (queryInterface, Sequelize) {
        var sqlRaw = 'ALTER TABLE `profile`' + 
                     ' ADD `profilePhotoId` VARCHAR(255),' + 
                     ' ADD CONSTRAINT `profile_profilePhotoId_idx`' +
                     ' FOREIGN KEY (`profilePhotoId`) REFERENCES `media` (`id`)' +
                     ' ON DELETE NO ACTION ON UPDATE NO ACTION;';
        return queryInterface.sequelize.query(sqlRaw).then(function () {
            return queryInterface.addColumn(
                'profile',
                'questionsCreated',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'questionsReviewed',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'questionTranslationsCreated',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'questionTranslationsReviewed',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'questionQuality',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
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
            return queryInterface.addColumn(
                'profile',
                'favoriteGame',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'issuesReported',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'lastActive',
                {
                    type: Sequelize.DATE,
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'profile',
                'preferences',
                {
                    type: Sequelize.TEXT,
                    allowNull: true
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'profile',
            'profilePhotoId'
        ).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'questionsCreated'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'questionsReviewed'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'questionTranslationsCreated'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'questionTranslationsReviewed'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'questionQuality'
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
            return queryInterface.removeColumn(
                'profile',
                'favoriteGame'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'issuesReported'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'lastActive'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'profile',
                'preferences'
            );
        });
    }
};