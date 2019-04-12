// Add new fields to Game: advertisement, advertisementProviderId, adsFrequencyAmount, adsFrequency
module.exports = {
    up: function (queryInterface, Sequelize) {
        var _constants = {
            ADS_FREQUENCY_BEFORE_THE_GAME: 'beforeTheGame',
            ADS_FREQUENCY_AFTER_THE_GAME: 'afterTheGame',
            ADS_FREQUENCY_AFTER_EACH_QUESTION: 'afterEachQuestion',
            ADS_FREQUENCY_AFTER_X_QUESTION: 'afterXQuestion'
        };
        return queryInterface.addColumn(
            'game',
            'advertisement',
            {
                type: Sequelize.INTEGER(1),
                allowNull: true,
                defaultValue: 0
            }
        ).then(function () {
            var sqlRaw = 'ALTER TABLE `game`' + 
                     ' ADD `advertisementProviderId` INTEGER(11),' + 
                     ' ADD CONSTRAINT `game_advertisementProviderId_idx`' +
                     ' FOREIGN KEY (`advertisementProviderId`) REFERENCES `advertisement_provider` (`id`)' +
                     ' ON DELETE NO ACTION ON UPDATE NO ACTION;';
            return queryInterface.sequelize.query(
                sqlRaw
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game',
                'adsFrequency',
                {
                    type: Sequelize.ENUM(
                        _constants.ADS_FREQUENCY_BEFORE_THE_GAME,
                        _constants.ADS_FREQUENCY_AFTER_THE_GAME,
                        _constants.ADS_FREQUENCY_AFTER_EACH_QUESTION,
                        _constants.ADS_FREQUENCY_AFTER_X_QUESTION
                    ),
                    allowNull: true,
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game',
                'adsFrequencyAmount',
                {
                    type: Sequelize.INTEGER(1),
                    allowNull: true
                }
            );
        });
        //JSON functions are supported only starting from MySQL 5.7
        //.then(function () {
        //    var sqlRaw = 'UPDATE `game`' + 
        //             '   SET `advertisement` = CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(`resultConfiguration`, \'$.advertisement\')) = "true" THEN 1 ELSE 0 END,' + 
        //             '       `advertisementProviderId` = CASE WHEN JSON_EXTRACT(`resultConfiguration`, \'$.advertisementProviderId\') > 0 THEN JSON_EXTRACT(`resultConfiguration`, \'$.advertisementProviderId\') ELSE NULL END,' + 
        //             '       `adsFrequency` = JSON_UNQUOTE(JSON_EXTRACT(`resultConfiguration`, \'$.adsFrequency\')),' + 
        //             '       `adsFrequencyAmount` = JSON_EXTRACT(`resultConfiguration`, \'$.adsFrequencyAmount\')' + 
        //             '  WHERE JSON_VALID(`resultConfiguration`);';
        //    return queryInterface.sequelize.query(
        //        sqlRaw
        //    );
        //});
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'game',
            'advertisement'
        ).then(function () {
            return queryInterface.removeColumn(
                'game',
                'advertisementProviderId'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'game',
                'adsFrequency'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'game',
                'adsFrequencyAmount'
            );
        });
    }
};