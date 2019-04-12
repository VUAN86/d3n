// add silentCountryCode field
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'profile',
            'silentCountryCode',
            {
                type: Sequelize.STRING,
                allowNull: true
            }
        );        
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn('profile', 'silentCountryCode');
    }
};