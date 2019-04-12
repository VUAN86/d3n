// Add mandatory creatorResourceId, createDate to application
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'application',
            'creatorResourceId',
            {
                type: Sequelize.STRING,
                allowNull: true
            }
        ).then(function () {
            return queryInterface.changeColumn(
                'application',
                'createDate',
                {
                    type: Sequelize.DATE,
                    allowNull: true,
                    defaultValue: Sequelize.NOW
                }
            );
        }).then(function () {
            var sqlRaw = 'UPDATE `application` SET `creatorResourceId` = ' + 
                         '(SELECT `profileId` FROM `profile_has_role` WHERE `profile_has_role`.`role` = \'ADMIN\' LIMIT 1)' +
                         ' WHERE `creatorResourceId` IS NULL;';
            return queryInterface.sequelize.query(
                sqlRaw
            );
        }).then(function () {
            var sqlRaw = 'UPDATE `application` SET `createDate` = NOW()' + 
                         ' WHERE `createDate` IS NULL;';
            return queryInterface.sequelize.query(
                sqlRaw
            );
        }).then(function () {
            return queryInterface.changeColumn(
                'application',
                'creatorResourceId',
                {
                    type: Sequelize.STRING,
                    allowNull: false
                }
            );
        }).then(function () {
            return queryInterface.changeColumn(
                'application',
                'createDate',
                {
                    type: Sequelize.DATE,
                    allowNull: false,
                    defaultValue: Sequelize.NOW
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'application',
            'creatorResourceId'
        ).then(function () {
            return queryInterface.changeColumn(
                'application',
                'createDate',
                {
                    type: Sequelize.DATE,
                    allowNull: true
                }
            );
        });
    }
};