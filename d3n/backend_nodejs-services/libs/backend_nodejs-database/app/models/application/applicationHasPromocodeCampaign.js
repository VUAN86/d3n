var _constants = {};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('applicationHasPromocodeCampaign', {
        applicationId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        promocodeCampaignId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        }
    }, {
        timestamps: false,
        tableName: 'application_has_promocode_campaign',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.application, { foreignKey: 'applicationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.promocodeCampaign, { foreignKey: 'promocodeCampaignId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
