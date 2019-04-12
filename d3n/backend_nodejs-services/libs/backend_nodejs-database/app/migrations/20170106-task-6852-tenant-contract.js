// Add tenant.tenantContract
module.exports = {
    up: function (queryInterface, Sequelize) {
        var _constants = {
            STATUS_ACTIVE: 'active',
            STATUS_INACTIVE: 'inactive',

            ACTION_NONE: 'none',
            ACTION_HOLD: 'hold',
            ACTION_CLOSE: 'close',
            ACTION_RENEW: 'renew',

            // tenant actions
            TYPE_CHANGE: 'dataChange',

            // contract actions
            TYPE_NONE: 'none',
            TYPE_HOLD_REQUEST: 'holdRequest',
            TYPE_CLOSE_REQUEST: 'closeRequest',
            TYPE_RENEW_REQUEST: 'renewContractRequest'
        };
        return queryInterface.createTable(
            'tenant_contract',
            {
                id: {
                    type: Sequelize.INTEGER(11),
                    primaryKey: true,
                    autoIncrement: true
                },
                tenantId: {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                },
                name: {
                    type: Sequelize.STRING,
                    allowNull: false
                },
                type: {
                    type: Sequelize.STRING,
                    allowNull: true
                },
                status: {
                    type: Sequelize.ENUM(
                        _constants.STATUS_ACTIVE,
                        _constants.STATUS_INACTIVE
                    ),
                    allowNull: false,
                    defaultValue: _constants.STATUS_ACTIVE,
                    comment: 'Current status of the contract, active or inactive.'
                },
                action: {
                    type: Sequelize.ENUM(
                        _constants.ACTION_NONE,
                        _constants.ACTION_HOLD,
                        _constants.ACTION_CLOSE,
                        _constants.ACTION_RENEW
                    ),
                    allowNull: false,
                    defaultValue: _constants.ACTION_NONE,
                    comment: 'The last action that was requested on the contract item.'
                },
                content: {
                    type: Sequelize.TEXT,
                    allowNull: true
                },
                startDate: {
                    type: Sequelize.DATE,
                    allowNull: false
                },
                endDate: {
                    type: Sequelize.DATE,
                    allowNull: false
                },
                createDate: {
                    type: Sequelize.DATE,
                    allowNull: false,
                    defaultValue: Sequelize.NOW
                },
                updateDate: {
                    type: Sequelize.DATE,
                    allowNull: false,
                    defaultValue: Sequelize.NOW
                },
            }
        ).then(function() {
            return queryInterface.createTable(
                'tenant_audit_log',
                {
                    id: {
                        type: Sequelize.INTEGER(11),
                        primaryKey: true,
                        autoIncrement: true
                    },
                    tenantId: {
                        type: Sequelize.INTEGER(11),
                        allowNull: false
                    },
                    creatorProfileId: {
                        type: Sequelize.STRING,
                        allowNull: false
                    },
                    type: {
                        type: Sequelize.ENUM(
                            _constants.TYPE_CHANGE
                        ),
                        allowNull: false,
                        defaultValue: _constants.TYPE_CHANGE
                    },
                    items: {
                        type: Sequelize.TEXT,
                        allowNull: true,
                    },
                    createDate: {
                        type: Sequelize.DATE,
                        allowNull: false,
                        defaultValue: Sequelize.NOW
                    },
                }
            );
        }).then(function() {
            return queryInterface.createTable(
                'tenant_contract_audit_log',
                {
                    id: {
                        type: Sequelize.INTEGER(11),
                        primaryKey: true,
                        autoIncrement: true
                    },
                    contractId: {
                        type: Sequelize.INTEGER(11),
                        allowNull: false
                    },
                    creatorProfileId: {
                        type: Sequelize.STRING,
                        allowNull: false
                    },
                    type: {
                        type: Sequelize.ENUM(
                            _constants.TYPE_NONE,
                            _constants.TYPE_HOLD_REQUEST,
                            _constants.TYPE_CLOSE_REQUEST,
                            _constants.TYPE_RENEW_REQUEST
                        ),
                        allowNull: false,
                        defaultValue: _constants.TYPE_NONE
                    },
                    items: {
                        type: Sequelize.TEXT,
                        allowNull: true,
                    },
                    createDate: {
                        type: Sequelize.DATE,
                        allowNull: false,
                        defaultValue: Sequelize.NOW
                    },
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.dropTable('tenant_audit_log')
          .then(function() {
            return queryInterface.dropTable('tenant_contract_audit_log');
          })
          .then(function() {
            return queryInterface.dropTable('tenant_contract');
        });
    }
};