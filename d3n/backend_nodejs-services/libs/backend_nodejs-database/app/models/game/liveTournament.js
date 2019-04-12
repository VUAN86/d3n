var _constants = {};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('liveTournament', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        name: {
            type: DataTypes.STRING,
            allowNull: true,
        }
    }, {
        timestamps: false,
        tableName: 'live_tournament',
        classMethods: {
            associate: function (models) {
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
