/*
 * configuration_setting table holds information about shared configurations
 * accross the whole app.
 */
var _constants = {
    ID_F4M_LEGAL: 'f4m_legal'
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('configuration_setting', {
        id: {
            type: DataTypes.STRING,
            primaryKey: true,
            comment: "Configuration setting object identifier."
        },
        data: {
            type: DataTypes.TEXT('long'),
            allowNull: false,
            comment: "JSON object that contains configuration settings.",
            set: function(val) {
                this.setDataValue('data', JSON.stringify(val));
            },
            get: function() {
                if (this.getDataValue('data')) {
                    return JSON.parse(this.getDataValue('data'));
                } else {
                    return null;
                }
            }
        },
    },
    {
        timestamps: false,
        tableName: 'configuration_setting',
        classMethods: {
            constants: function () {
                return _constants;
            }
        }
    });
};
