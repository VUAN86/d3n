// Add fields to teant: contactFirstName, contactLastName
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'tenant',
            'contactFirstName',
            {
                type: Sequelize.STRING,
                allowNull: true
            }
        ).then(function () {
            return queryInterface.addColumn(
                'tenant',
                'contactLastName',
                {
                    type: Sequelize.STRING,
                    allowNull: true
                }
            );
        }).then(function () {
            var sqlRaw =" UPDATE `tenant` SET " +
                "`contactFirstName` = IF( LOCATE(' ', `contactPerson`) > 0, SUBSTRING(`contactPerson`, 1, LOCATE(' ', `contactPerson`) - 1), `contactPerson`), " +
                "`contactLastName` = IF(LOCATE(' ', `contactPerson`) > 0, SUBSTRING(`contactPerson`, LOCATE(' ', `contactPerson`) + 1), NULL); ";


            return queryInterface.sequelize.query(
                sqlRaw
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'tenant',
                'contactPerson'
            );
        });
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
                'tenant',
                'contactPerson',
                {
                    type: Sequelize.STRING,
                    allowNull: true
                }
        ).then(function () {
            var sqlRaw =" UPDATE `tenant` SET " +
                "`contactPerson` = CONCAT(contactFirstName, ' ', contactLastName);";
            return queryInterface.sequelize.query(
                sqlRaw
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'tenant',
                'contactLastName'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'tenant',
                'contactFirstName'
            );
        });
    }
};