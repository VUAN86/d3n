// update Application.configuration data
module.exports = {
    up: function (queryInterface, Sequelize) {
        var sqlRaw = "UPDATE `application` SET `configuration` = REPLACE(`configuration`, 'firstTimeSignUpBonusPoints', 'simpleRegistrationBonusPoints') WHERE `configuration` LIKE '%firstTimeSignUpBonusPoints%';";
        return queryInterface.sequelize.query(sqlRaw, { raw: true }).then(function () {
            var sqlRaw = "UPDATE `application` SET `configuration` = REPLACE(`configuration`, 'firstTimeSignUpCredits', 'simpleRegistrationCredits') WHERE `configuration` LIKE '%firstTimeSignUpCredits%';";
            return queryInterface.sequelize.query(sqlRaw, { raw: true });
        }).then(function () {
            var sqlRaw = "UPDATE `application` SET `configuration` = REPLACE(`configuration`, SUBSTRING(configuration, 2, LENGTH(configuration) - 2), CONCAT(SUBSTRING(configuration, 2, LENGTH(configuration) - 2), ',\"fullRegistrationBonusPoints\":1,\"fullRegistrationCredits\":1')) WHERE `configuration` LIKE '%simpleRegistration%';";
            return queryInterface.sequelize.query(sqlRaw, { raw: true });
        });        
    },
    
    down: function (queryInterface, Sequelize) {
        return true;
    }
};