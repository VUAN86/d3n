// Change application/advertisement/voucher.status: add archived, set default value to active
// Add game.status: active, inactive, archived
module.exports = {
    up: function (queryInterface, Sequelize) {
        var _constants = {
            STATUS_ACTIVE: 'active',
            STATUS_INACTIVE: 'inactive',
            STATUS_ARCHIVED: 'archived'
        };
        return queryInterface.changeColumn(
            'application',
            'status',
            {
                type: Sequelize.ENUM(
                    _constants.STATUS_ACTIVE,
                    _constants.STATUS_INACTIVE,
                    _constants.STATUS_ARCHIVED
                ),
                allowNull: true,
                defaultValue: _constants.STATUS_ACTIVE
            }
        ).then(function () {
            return queryInterface.addColumn(
                'game',
                'status',
                {
                    type: Sequelize.ENUM(
                        _constants.STATUS_ACTIVE,
                        _constants.STATUS_INACTIVE,
                        _constants.STATUS_ARCHIVED
                    ),
                    allowNull: true,
                    defaultValue: _constants.STATUS_ACTIVE
                }
            );
        }).then(function () {
            return queryInterface.changeColumn(
                'advertisement',
                'status',
                {
                    type: Sequelize.ENUM(
                        _constants.STATUS_ACTIVE,
                        _constants.STATUS_INACTIVE,
                        _constants.STATUS_ARCHIVED
                    ),
                    allowNull: true,
                    defaultValue: _constants.STATUS_ACTIVE
                }
            );
        }).then(function () {
            return queryInterface.changeColumn(
                'voucher',
                'status',
                {
                    type: Sequelize.ENUM(
                        _constants.STATUS_ACTIVE,
                        _constants.STATUS_INACTIVE,
                        _constants.STATUS_ARCHIVED
                    ),
                    allowNull: true,
                    defaultValue: _constants.STATUS_ACTIVE
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        var _constants = {
            STATUS_ACTIVE: 'active',
            STATUS_INACTIVE: 'inactive'
        };
        return queryInterface.changeColumn(
            'application',
            'status',
            {
                type: Sequelize.ENUM(
                    _constants.STATUS_ACTIVE,
                    _constants.STATUS_INACTIVE
                ),
                allowNull: true,
                defaultValue: _constants.STATUS_ACTIVE
            }
        ).then(function () {
            return queryInterface.removeColumn(
                'game',
                'status'
            );
        }).then(function () {
            return queryInterface.changeColumn(
                'advertisement',
                'status',
                {
                    type: Sequelize.ENUM(
                        _constants.STATUS_ACTIVE,
                        _constants.STATUS_INACTIVE
                    ),
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.changeColumn(
                'voucher',
                'status',
                {
                    type: Sequelize.ENUM(
                        _constants.STATUS_ACTIVE,
                        _constants.STATUS_INACTIVE
                    ),
                    allowNull: true
                }
            );
        });
    }
};