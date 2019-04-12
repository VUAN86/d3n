// Move references to regionalSetting from advertisement/voucher models to advertisementProvider/voucherProvider
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.dropTable(
            'advertisement_has_regional_setting'
        ).then(function () {
            return queryInterface.dropTable(
                'voucher_has_regional_setting'
            );
        }).then(function () {
            return queryInterface.createTable(
                'advertisement_provider_has_regional_setting',
                {
                    advertisementProviderId: {
                        type: Sequelize.INTEGER(11),
                        allowNull: false,
                        primaryKey: true,
                        references: {
                            model: 'advertisement_provider',
                            key: 'id'
                        },
                        field: 'advertisementProviderId',
                        onUpdate: 'NO ACTION',
                        onDelete: 'NO ACTION'
                    },
                    regionalSettingId: {
                        type: Sequelize.INTEGER(11),
                        allowNull: false,
                        primaryKey: true,
                        references: {
                            model: 'regional_setting',
                            key: 'id'
                        },                        
                        field: 'regionalSettingId',
                        onUpdate: 'NO ACTION',
                        onDelete: 'NO ACTION'
                    }
                }
            );
        }).then(function () {
            return queryInterface.createTable(
                'voucher_provider_has_regional_setting',
                {
                    voucherProviderId: {
                        type: Sequelize.INTEGER(11),
                        allowNull: false,
                        primaryKey: true,
                        references: {
                            model: 'voucher_provider',
                            key: 'id'
                        },                        
                        field: 'voucherProviderId',
                        onUpdate: 'NO ACTION',
                        onDelete: 'NO ACTION'
                    },
                    regionalSettingId: {
                        type: Sequelize.INTEGER(11),
                        allowNull: false,
                        primaryKey: true,
                        references: {
                            model: 'regional_setting',
                            key: 'id'
                        },                        
                        field: 'regionalSettingId',
                        onUpdate: 'NO ACTION',
                        onDelete: 'NO ACTION'
                    }
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.dropTable(
            'advertisement_provider_has_regional_setting'
        ).then(function () {
            return queryInterface.dropTable(
                'voucher_provider_has_regional_setting'
            );
        }).then(function () {
            return queryInterface.createTable(
                'advertisement_has_regional_setting',
                {
                    advertisementId: {
                        type: Sequelize.INTEGER(11),
                        allowNull: false,
                        primaryKey: true,
                        references: {
                            model: 'advertisement',
                            key: 'id'
                        },
                        field: 'advertisementId',
                        onUpdate: 'NO ACTION',
                        onDelete: 'NO ACTION'
                    },
                    regionalSettingId: {
                        type: Sequelize.INTEGER(11),
                        allowNull: false,
                        primaryKey: true,
                        references: {
                            model: 'regional_setting',
                            key: 'id'
                        },                        
                        field: 'regionalSettingId',
                        onUpdate: 'NO ACTION',
                        onDelete: 'NO ACTION'
                    }
                }
            );
        }).then(function () {
            return queryInterface.createTable(
                'voucher_has_regional_setting',
                {
                    voucherId: {
                        type: Sequelize.INTEGER(11),
                        allowNull: false,
                        primaryKey: true,
                        references: {
                            model: 'voucher',
                            key: 'id'
                        },                        
                        field: 'voucherId',
                        onUpdate: 'NO ACTION',
                        onDelete: 'NO ACTION'
                    },
                    regionalSettingId: {
                        type: Sequelize.INTEGER(11),
                        allowNull: false,
                        primaryKey: true,
                        references: {
                            model: 'regional_setting',
                            key: 'id'
                        },                        
                        field: 'regionalSettingId',
                        onUpdate: 'NO ACTION',
                        onDelete: 'NO ACTION'
                    }
                }
            );
        });
    }
};