// add publishIdx field
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'advertisement',
            'publishIdx',
            {
                type: Sequelize.INTEGER(11),
                allowNull: true
            }
        );        
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn('advertisement', 'publishIdx');
    }
};