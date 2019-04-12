// Add profile.organization
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'profile',
            'organization',
            {
                type: Sequelize.STRING,
                allowNull: true,
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'profile',
            'organization'
        );
    }
};