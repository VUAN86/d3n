// 
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.sequelize.query(
            "INSERT IGNORE INTO tenant (id, name, businessPlan, description)" + 
            " VALUES ( 1, 'Tenant Developer Test', 'No plan', " + 
            "'First tenant was created by developers as part of migrations process')"
        );
    },
    
    down: function (queryInterface, Sequelize) {
        // Leaving first tenant as is, not cleaning up
    }
};