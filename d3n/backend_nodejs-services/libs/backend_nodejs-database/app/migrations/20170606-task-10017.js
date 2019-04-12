// add profile role fields: requestDate, requestDate
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'profile_has_role',
            'requestDate',
            {
                type: Sequelize.DATE,
                allowNull: true
            }
        ).then(function () {
            return queryInterface.addColumn(
                'profile_has_role',
                'acceptanceDate',
                {
                    type: Sequelize.DATE,
                    allowNull: true
                }
            );
        }).then(function () {
            var sqlRaw = "UPDATE `profile_has_role` SET `requestDate` = CURRENT_TIMESTAMP WHERE `status` = 'applied';";
            return queryInterface.sequelize.query(sqlRaw, { raw: true });
        }).then(function () {
            var sqlRaw = "UPDATE `profile_has_role` SET `acceptanceDate` = CURRENT_TIMESTAMP WHERE `status` = 'approved';";
            return queryInterface.sequelize.query(sqlRaw, { raw: true });
        });        
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn('profile_has_role', 'requestDate').then(function() {
            return queryInterface.removeColumn('profile_has_role', 'acceptanceDate');
        });
    }
};