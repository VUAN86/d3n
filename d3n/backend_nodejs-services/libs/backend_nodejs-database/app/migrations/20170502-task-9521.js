module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'profile',
            'createDate',
            {
                type: Sequelize.DATE,
                allowNull: true
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'profile',
            'createDate'
        );
    }
};