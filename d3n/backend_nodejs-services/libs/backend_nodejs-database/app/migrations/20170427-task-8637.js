module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'game',
            'isAutoValidationEnabled',
            {
                type: Sequelize.INTEGER(1),
                allowNull: true,
                defaultValue: 1
            }
        ).then(function () {
            return queryInterface.sequelize.query('UPDATE `game` SET `isAutoValidationEnabled` = 1 WHERE `isAutoValidationEnabled` IS NULL;');
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'game',
            'isAutoValidationEnabled'
        );
    }
};