// Add missing fields to tombola, remove obsolete
var logger = require('nodejs-logger')();

var _constants = {
    STATUS_INACTIVE: 'inactive',
    STATUS_ACTIVE: 'active',
    STATUS_DIRTY: 'dirty',
    STATUS_ARCHIVED: 'archived',
    STATUS_EXPIRED: 'expired'
};

module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'tombola',
            'isDeployed'
        ).catch(function (err) {
        }).then(function () {
            return queryInterface.changeColumn(
                'tombola',
                'status',
                {
                    type: Sequelize.ENUM(
                        _constants.STATUS_INACTIVE,
                        _constants.STATUS_ACTIVE,
                        _constants.STATUS_DIRTY,
                        _constants.STATUS_ARCHIVED,
                        _constants.STATUS_EXPIRED
                    ),
                    allowNull: true,
                    defaultValue: _constants.STATUS_INACTIVE
                }
            ).catch(function (err) {});
        }).then(function () {
            return queryInterface.addColumn(
                'tombola',
                'updateDate',
                {
                    type: Sequelize.DATE,
                    allowNull: true
                }
            ).catch(function (err) {});
        }).then(function () {
            return queryInterface.addColumn(
                'tombola',
                'version',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 1
                }
            ).catch(function (err) {});
        }).then(function () {
            return queryInterface.addColumn(
                'tombola',
                'publishedVersion',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: true
                }
            ).catch(function (err) {});
        }).then(function () {
            return queryInterface.addColumn(
                'tombola',
                'publishingDate',
                {
                    type: Sequelize.DATE,
                    allowNull: true
                }
            ).catch(function (err) {});
        }).then(function () {
            var sqlRaw = 'ALTER TABLE `tombola`' + 
                         ' ADD CONSTRAINT `tombola_tenantId_idx`' +
                         ' FOREIGN KEY (`tenantId`) REFERENCES `tenant` (`id`)' +
                         ' ON DELETE NO ACTION ON UPDATE NO ACTION;';
            return queryInterface.sequelize.query(sqlRaw).catch(function (err) {});
        });
    },

    down: function (queryInterface, Sequelize) {
        return true;
    }
};