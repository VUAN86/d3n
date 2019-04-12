var _constants = {
    STATUS_ACTIVE: 'active',
    STATUS_INACTIVE: 'inactive',
};

var Tenant = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('tenant', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        name: {
            type: DataTypes.STRING,
            allowNull: false,
            comment: 'The name of the tenant'
        },
        url: {
            type: DataTypes.STRING,
            allowNull: true,
            comment: 'URL for the website/domain'
        },
        autoCommunity: {
            type: DataTypes.INTEGER(1),
            allowNull: false,
            defaultValue: 0
        },
        logoUrl: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        createDate: {
            type: DataTypes.DATE,
            allowNull: false,
            defaultValue: sequelize.NOW
        },
        updateDate: {
            type: DataTypes.DATE,
            allowNull: false,
            defaultValue: sequelize.NOW
        },
        status: {
            type: DataTypes.ENUM(
                _constants.STATUS_ACTIVE,
                _constants.STATUS_INACTIVE
            ),
            allowNull: false,
            defaultValue: _constants.STATUS_ACTIVE
        },
        address: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        city: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        country: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        vat: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        email: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        phone: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        contactFirstName: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        contactLastName: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        description: {
            type: DataTypes.TEXT,
            allowNull: true,
        },
        currency: {
            type: DataTypes.STRING(3),
            allowNull: true,
        }
    }, {
        timestamps: false,
        tableName: 'tenant',
        classMethods: {
            associate: function (models) {
                this.hasMany(models.achievement, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.badge, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.advertisementProvider, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.application, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.paymentStructure, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.paymentTenantBillOfMaterial, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.profileHasRole, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.profileHasStats, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.language, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.poolHasTenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.questionTemplate, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.game, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.tag, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.voucherProvider, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.winningComponent, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.tombola, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.promocodeCampaign, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.workorderHasTenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.tenantContract, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.tenantAuditLog, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.tenantInvoice, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
