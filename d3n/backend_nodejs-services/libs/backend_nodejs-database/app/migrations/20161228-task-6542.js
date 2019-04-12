// Extend primary key with action
module.exports = {
    up: function (queryInterface, Sequelize) {
        var sqlRaw = 'ALTER TABLE workorder_has_resource ' + 
                     'DROP PRIMARY KEY, ' +
                     'ADD PRIMARY KEY(workorderId, resourceId, action);';
        return queryInterface.sequelize.query(sqlRaw);
    },
    
    down: function (queryInterface, Sequelize) {
        // do nothing
    }
};