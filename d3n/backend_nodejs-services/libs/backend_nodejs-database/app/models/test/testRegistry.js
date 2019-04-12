var _constants = {};

var TestRegistry = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('testRegistry', {
        serviceName: {
            type: DataTypes.STRING,
            allowNull: false,
            primaryKey: true,
        },
        availableSets: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
        },
        pulledSets: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
        }
    },
        {
            timestamps: false,
            tableName: '$test_registry',
            classMethods: {
                associate: function (models) {
                },
                constants: function () {
                    return _constants;
                }
            }
        });
};
