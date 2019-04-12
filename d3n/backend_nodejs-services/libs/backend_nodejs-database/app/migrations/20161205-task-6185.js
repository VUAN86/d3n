// Add profileHasGlobalRole table
module.exports = {
    up: function (queryInterface, Sequelize) {
        var _constants = {
            ROLE_ANONYMOUS: 'ANONYMOUS',
            ROLE_REGISTERED: 'REGISTERED',
            ROLE_FULLY_REGISTERED: 'FULLY_REGISTERED',
            ROLE_FULLY_REGISTERED_BANK: 'FULLY_REGISTERED_BANK',
            ROLE_FULLY_REGISTERED_BANK_O18: 'FULLY_REGISTERED_BANK_O18'
        };
        return queryInterface.createTable(
            'profile_has_global_role',
            {
                id: {
                    type: Sequelize.INTEGER(11),
                    primaryKey: true,
                    autoIncrement: true,
                },
                profileId: {
                    type: Sequelize.STRING,
                    allowNull: false,
                    comment: "The id of the user in the aerospike database.",
                    unique: "profileGlobalRole"
                },
                role: {
                    type: Sequelize.ENUM(
                        _constants.ROLE_ANONYMOUS,
                        _constants.ROLE_REGISTERED,
                        _constants.ROLE_FULLY_REGISTERED,
                        _constants.ROLE_FULLY_REGISTERED_BANK,
                        _constants.ROLE_FULLY_REGISTERED_BANK_O18
                    ),
                    allowNull: false,
                    comment: "Role identifier string, these are roles.",
                    unique: "profileGlobalRole"
                }
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.dropTable(
            'profile_has_global_role'
        );
    }
};