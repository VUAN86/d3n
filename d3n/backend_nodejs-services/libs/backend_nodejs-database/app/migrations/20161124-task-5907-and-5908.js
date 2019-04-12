// Change values for status field of advertisement_provider/voucher_provider: active, inaactive, archived
module.exports = {
    up: function (queryInterface, Sequelize) {
        var _constants = {
            STATUS_ACTIVE: 'active',
            STATUS_INACTIVE: 'inactive',
            STATUS_ARCHIVED: 'archived'
        };
        return queryInterface.changeColumn(
            'advertisement_provider',
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
            return queryInterface.changeColumn(
                'voucher_provider',
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
            'advertisement_provider',
            'status',
            {
                type: Sequelize.ENUM(
                    _constants.STATUS_ACTIVE,
                    _constants.STATUS_INACTIVE
                ),
                allowNull: true
            }
        ).then(function () {
            return queryInterface.changeColumn(
                'voucher_provider',
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