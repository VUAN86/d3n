// Change game/status to be inactive when created
module.exports = {
    up: function (queryInterface, Sequelize) {
        var _constants = {
            STATUS_ACTIVE: 'active',
            STATUS_INACTIVE: 'inactive',
            STATUS_ARCHIVED: 'archived'
        };
        return queryInterface.changeColumn(
            'game',
            'status',
            {
                type: Sequelize.ENUM(
                    _constants.STATUS_ACTIVE,
                    _constants.STATUS_INACTIVE,
                    _constants.STATUS_ARCHIVED
                ),
                allowNull: true,
                defaultValue: _constants.STATUS_INACTIVE
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        var _constants = {
            STATUS_ACTIVE: 'active',
            STATUS_INACTIVE: 'inactive',
            STATUS_ARCHIVED: 'archived'
        };
        return queryInterface.changeColumn(
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
    }
};