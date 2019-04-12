// Change values for status field of advertisement_provider/voucher_provider: active, inaactive, archived
module.exports = {
    up: function (queryInterface, Sequelize) {
        var _constants = {
            STATUS_ACTIVE: 'active',
            STATUS_INACTIVE: 'inactive',
        };
        return queryInterface.dropTable(
            'tenant_has_resource'
        ).then(function () {
            return queryInterface.addColumn(
                'voucher_provider',
                'tenantId',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: true,
                    references: {
                        model: 'tenant',
                        key: 'id'
                    },
                    onUpdate: 'NO ACTION',
                    onDelete: 'NO ACTION'
                }
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'tenant',
                'businessPlan'
            );
        }).then(function () {
            return queryInterface.addColumn(
                'tenant',
                'logoUrl',
                {
                    type: Sequelize.STRING,
                    allowNull: true,
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'tenant',
                'createDate',
                {
                    type: Sequelize.DATE,
                    allowNull: false,
                    defaultValue: Sequelize.NOW
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'tenant',
                'updateDate',
                {
                    type: Sequelize.DATE,
                    allowNull: false,
                    defaultValue: Sequelize.NOW
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'tenant',
                'status',
                {
                    type: Sequelize.ENUM(
                        _constants.STATUS_ACTIVE,
                        _constants.STATUS_INACTIVE
                    ),
                    allowNull: false,
                    defaultValue: _constants.STATUS_ACTIVE
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'tenant',
                'address',
                {
                    type: Sequelize.STRING,
                    allowNull: true,
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'tenant',
                'city',
                {
                    type: Sequelize.STRING,
                    allowNull: true,
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'tenant',
                'country',
                {
                    type: Sequelize.STRING,
                    allowNull: true,
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'tenant',
                'vat',
                {
                    type: Sequelize.STRING,
                    allowNull: true,
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'tenant',
                'url',
                {
                    type: Sequelize.STRING,
                    allowNull: true,
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'tenant',
                'email',
                {
                    type: Sequelize.STRING,
                    allowNull: true,
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'tenant',
                'phone',
                {
                    type: Sequelize.STRING,
                    allowNull: true,
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'tenant',
                'contactPerson',
                {
                    type: Sequelize.STRING,
                    allowNull: true,
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        // Don't revert back table tenantHasResource - it is obsolete and not used anywhere
        // Don't revert back table voucherProvider
        return queryInterface.addColumn(
            'tenant',
            'businessPlan',
            {
                type: Sequelize.STRING,
                allowNull: true,
            }
        ).then(function () {
            return queryInterface.removeColumn(
                'tenant',
                'logoUrl'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'tenant',
                'createDate'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'tenant',
                'updateDate'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'tenant',
                'status'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'tenant',
                'address'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'tenant',
                'city'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'tenant',
                'country'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'tenant',
                'vat'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'tenant',
                'url'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'tenant',
                'email'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'tenant',
                'phone'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'tenant',
                'contactPerson'
            );
        });
    }
};