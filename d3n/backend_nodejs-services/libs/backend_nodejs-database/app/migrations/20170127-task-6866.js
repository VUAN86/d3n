// Refactor statuses
var logger = require('nodejs-logger')();

var _constants = {
    STATUS_INACTIVE: 'inactive',
    STATUS_ACTIVE: 'active',
    STATUS_DIRTY: 'dirty',
    STATUS_ARCHIVED: 'archived'
};

module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'promocode_campaign',
            'createDate'
        ).catch(function (err) {}).then(function () {
            return queryInterface.removeColumn(
                'promocode_campaign',
                'updateDate'
            ).catch(function (err) {});
        }).then(function () {
            return queryInterface.addColumn(
                'promocode_campaign',
                'tenantId',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: true
                }
            ).catch(function (err) {});
        }).then(function () {
            return queryInterface.addColumn(
                'promocode_campaign',
                'status',
                {
                    type: Sequelize.ENUM(
                        _constants.STATUS_INACTIVE,
                        _constants.STATUS_ACTIVE,
                        _constants.STATUS_DIRTY,
                        _constants.STATUS_ARCHIVED
                    ),
                    allowNull: true,
                    defaultValue: _constants.STATUS_INACTIVE
                }
            ).catch(function (err) {});
        }).then(function () {
            return queryInterface.addColumn(
                'promocode_campaign',
                'createDate',
                {
                    type: Sequelize.DATE,
                    allowNull: false,
                    defaultValue: Sequelize.NOW
                }
            ).catch(function (err) {});
        }).then(function () {
            return queryInterface.addColumn(
                'promocode_campaign',
                'updateDate',
                {
                    type: Sequelize.DATE,
                    allowNull: true
                }
            ).catch(function (err) {});
        }).then(function () {
            return queryInterface.addColumn(
                'promocode_campaign',
                'version',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 1
                }
            ).catch(function (err) {});
        }).then(function () {
            return queryInterface.addColumn(
                'promocode_campaign',
                'publishedVersion',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: true
                }
            ).catch(function (err) {});
        }).then(function () {
            return queryInterface.addColumn(
                'promocode_campaign',
                'publishingDate',
                {
                    type: Sequelize.DATE,
                    allowNull: true
                }
            ).catch(function (err) {});
        });
    },

    down: function (queryInterface, Sequelize) {
        return true;
    }
};