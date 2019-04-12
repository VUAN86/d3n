// Fix statuses for Application, Game, Advertisement, Voucher
var logger = require('nodejs-logger')();

var _constants = {
    STATUS_INACTIVE: 'inactive',
    STATUS_ACTIVE: 'active',
    STATUS_DIRTY: 'dirty',
    STATUS_ARCHIVED: 'archived'
};

module.exports = {
    up: function (queryInterface, Sequelize) {
        return _adjustStatus(
            queryInterface, Sequelize, 'advertisement'
        ).then(function () {
            return _adjustStatus(queryInterface, Sequelize, 'application');
        }).then(function () {
            return _adjustStatus(queryInterface, Sequelize, 'game');
        }).then(function () {
            return _adjustStatus(queryInterface, Sequelize, 'voucher');
        });
    },

    down: function (queryInterface, Sequelize) {
        return true;
    }
};

function _adjustStatus(queryInterface, Sequelize, model) {
    logger.warn('Adjusting status of ' + model);
    return queryInterface.changeColumn(
        model,
        'status',
        {
            type: Sequelize.ENUM(
                _constants.STATUS_INACTIVE,
                _constants.STATUS_ACTIVE,
                _constants.STATUS_DIRTY,
                _constants.STATUS_ARCHIVED
            ),
            allowNull: true,
            defaultValue: _constants.STATUS_INACTIVE
        }
    );
}