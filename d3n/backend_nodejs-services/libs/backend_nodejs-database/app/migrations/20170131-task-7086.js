// Adjust field ProfileHasGlobalRole.role: add ROLE_NOT_VALIDATED
module.exports = {
    up: function (queryInterface, Sequelize) {
        var _constants = {
            ROLE_ANONYMOUS: 'ANONYMOUS',
            ROLE_NOT_VALIDATED: 'NOT_VALIDATED',
            ROLE_REGISTERED: 'REGISTERED',
            ROLE_FULLY_REGISTERED: 'FULLY_REGISTERED',
            ROLE_FULLY_REGISTERED_BANK: 'FULLY_REGISTERED_BANK',
            ROLE_FULLY_REGISTERED_BANK_O18: 'FULLY_REGISTERED_BANK_O18'
        };
        return queryInterface.changeColumn(
            'profile_has_global_role',
            'role',
            {
                type: Sequelize.ENUM(
                    _constants.ROLE_ANONYMOUS,
                    _constants.ROLE_NOT_VALIDATED,
                    _constants.ROLE_REGISTERED,
                    _constants.ROLE_FULLY_REGISTERED,
                    _constants.ROLE_FULLY_REGISTERED_BANK,
                    _constants.ROLE_FULLY_REGISTERED_BANK_O18
                ),
                allowNull: false,
                comment: "Role identifier string, these are roles.",
                unique: "profileGlobalRole"
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return true;
    }
};