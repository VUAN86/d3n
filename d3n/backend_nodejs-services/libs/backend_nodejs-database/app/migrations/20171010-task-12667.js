// Game: add playerEntryFeeType enum field; add playerEntryFeeValues JSON BLOB field
module.exports = {
    up: function (queryInterface, Sequelize) {
        var _constants = {
            ENTRY_FEE_TYPE_STEP: 'step',
            ENTRY_FEE_TYPE_FIXED: 'fixed'    
        };
        return queryInterface.addColumn(
            'game',
            'playerEntryFeeType',
            {
                type: Sequelize.ENUM(
                    _constants.ENTRY_FEE_TYPE_STEP,
                    _constants.ENTRY_FEE_TYPE_FIXED
                ),
                allowNull: false,
                defaultValue: _constants.ENTRY_FEE_TYPE_STEP
            }
        ).then(function () {
            return queryInterface.addColumn(
                'game',
                'playerEntryFeeValues',
                {
                    type: Sequelize.TEXT,
                    allowNull: true
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'game',
            'playerEntryFeeType'
        ).then(function () {
            return queryInterface.removeColumn(
                'game',
                'playerEntryFeeValues'
            );
        });
    }
};