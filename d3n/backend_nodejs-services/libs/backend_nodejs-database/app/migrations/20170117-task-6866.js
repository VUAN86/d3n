// Refactor statuses
var logger = require('nodejs-logger')();

var _constants = {
    STATUS_DRAFT: 'draft',
    STATUS_REVIEW: 'review',
    STATUS_APPROVED: 'approved',
    STATUS_DECLINED: 'declined',
    STATUS_INACTIVE: 'inactive',
    STATUS_ACTIVE: 'active',
    STATUS_DIRTY: 'dirty',
    STATUS_ARCHIVED: 'archived',
    STATUS_UNPUBLISHED: 'unpublished',
    STATUS_PUBLISHED: 'published',
    STATUS_REPUBLISHED: 'republished'
};

module.exports = {
    up: function (queryInterface, Sequelize) {
        return _removeIsDeleted(
            queryInterface, Sequelize, 'question'
        ).then(function () {
            return _removeIsDeployed(queryInterface, Sequelize, 'advertisement');
        }).then(function () {
            return _removeIsDeployed(queryInterface, Sequelize, 'application');
        }).then(function () {
            return _removeIsDeployed(queryInterface, Sequelize, 'game');
        }).then(function () {
            return _removeIsDeployed(queryInterface, Sequelize, 'voucher');
        }).then(function () {
            return _removeDeploymentStatusAndDate(queryInterface, Sequelize, 'question');
        }).then(function () {
            return _removeDeploymentStatusAndDate(queryInterface, Sequelize, 'question_translation');
        }).then(function () {
            return _addFields(queryInterface, Sequelize, 'advertisement');
        }).then(function () {
            return _addFields(queryInterface, Sequelize, 'application');
        }).then(function () {
            return _addFields(queryInterface, Sequelize, 'game');
        }).then(function () {
            return _addFields(queryInterface, Sequelize, 'voucher');
        }).then(function () {
            return _addFields(queryInterface, Sequelize, 'question');
        }).then(function () {
            return _addFields(queryInterface, Sequelize, 'question_translation');
        });
    },

    down: function (queryInterface, Sequelize) {
        return true;
    }
};

function _removeIsDeleted(queryInterface, Sequelize, model) {
    logger.warn('Removing isDeleted from ' + model);
    var sqlRaw = 'UPDATE `' + model + '` SET `status` = \'' + _constants.STATUS_ARCHIVED + '\' WHERE `isDeleted` = 1;';
    return queryInterface.sequelize.query(
        sqlRaw
    ).then(function () {
        return queryInterface.removeColumn(
            model,
            'isDeleted'
        );
    });
}

function _removeIsDeployed(queryInterface, Sequelize, model) {
    logger.warn('Removing isDeployed from ' + model);
    var sqlRaw = 'UPDATE `' + model + '` SET `status` = \'' + _constants.STATUS_ACTIVE + '\' WHERE `isDeployed` = 1;';
    return queryInterface.sequelize.query(
        sqlRaw
    ).then(function () {
        return queryInterface.removeColumn(
            model,
            'isDeployed'
        );
    });
}

function _removeDeploymentStatusAndDate(queryInterface, Sequelize, model) {
    logger.warn('Removing deploymentStatus, deploymentDate from ' + model);
    var sqlRaw = 'UPDATE `' + model + '` SET `status` = \'' + _constants.STATUS_PUBLISHED + '\' WHERE `deploymentStatus` = \'' + _constants.STATUS_PUBLISHED + '\';';
    return queryInterface.sequelize.query(
        sqlRaw
    ).then(function () {
        return queryInterface.removeColumn(
            model,
            'deploymentStatus'
        );
    }).then(function () {
        return queryInterface.removeColumn(
            model,
            'deploymentDate'
        );
    });
}

function _addFields(queryInterface, Sequelize, model) {
    var result;
    // Add version
    logger.warn('Adding version to ' + model);
    result = queryInterface.addColumn(
        model,
        'version',
        {
            type: Sequelize.INTEGER(11),
            allowNull: true,
            defaultValue: 1
        }
    ).then(function () {
        logger.warn('Set initial version to ' + model);
        var sqlRaw = 'UPDATE `' + model + '` SET `version` = 1;';
        return queryInterface.sequelize.query(
            sqlRaw
        );
    });
    // Add createDate if not exists
    if (model !== 'application' && model !== 'question' && model !== 'question_translation') {
        result = result.then(function () {
            logger.warn('Add createDate to ' + model);
            return queryInterface.addColumn(
                model,
                'createDate',
                {
                    type: Sequelize.DATE,
                    allowNull: true,
                    defaultValue: Sequelize.NOW
                }
            );
        });
    };
    // Add updateDate, publishedVersion, publishingDate
    result = result.then(function () {
        logger.warn('Add updateDate to ' + model);
        return queryInterface.addColumn(
            model,
            'updateDate',
            {
                type: Sequelize.DATE,
                allowNull: true
            }
        );
    }).then(function () {
        logger.warn('Add publishedVersion to ' + model);
        return queryInterface.addColumn(
            model,
            'publishedVersion',
            {
                type: Sequelize.INTEGER(11),
                allowNull: true
            }
        );
    }).then(function () {
        logger.warn('Add publishingDate to ' + model);
        return queryInterface.addColumn(
            model,
            'publishingDate',
            {
                type: Sequelize.DATE,
                allowNull: true
            }
        );
    }).then(function () {
        logger.warn('Update publishedVersion, publishingDate for ' + model);
        var sqlRaw = 'UPDATE `' + model + '` SET `publishedVersion` = 1, publishingDate = NOW() WHERE `status` = \'' + _constants.STATUS_ACTIVE + '\' OR `status` = \'' + _constants.STATUS_PUBLISHED + '\';';
        return queryInterface.sequelize.query(
            sqlRaw
        );
    });
    // Change status if needed
    result = result.then(function () {
        if (model.startsWith('question')) {
            logger.warn('Append active/inactive/dirty statuses for ' + model);
            return queryInterface.changeColumn(
                model,
                'status',
                {
                    type: Sequelize.ENUM(
                        _constants.STATUS_DRAFT,
                        _constants.STATUS_REVIEW,
                        _constants.STATUS_APPROVED,
                        _constants.STATUS_DECLINED,
                        _constants.STATUS_INACTIVE,
                        _constants.STATUS_ACTIVE,
                        _constants.STATUS_DIRTY,
                        _constants.STATUS_ARCHIVED,
                        _constants.STATUS_UNPUBLISHED,
                        _constants.STATUS_PUBLISHED,
                        _constants.STATUS_REPUBLISHED
                    ),
                    allowNull: true,
                    defaultValue: _constants.STATUS_DRAFT
                }
            );
        } else {
            logger.warn('Add dirty status for ' + model);
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
    });
    // Migrate published/unpublished/republished statuses to active/inactive
    result = result.then(function () {
        logger.warn('Update published/republished status to active for ' + model);
        var sqlRaw = 'UPDATE `' + model + '` SET `status` = \'' + _constants.STATUS_ACTIVE + '\' WHERE `status` = \'' + _constants.STATUS_PUBLISHED + '\' OR `status` = \'' + _constants.STATUS_REPUBLISHED + '\';';
        return queryInterface.sequelize.query(
            sqlRaw
        );
    }).then(function () {
        logger.warn('Update unpublished status to inactive for ' + model);
        var sqlRaw = 'UPDATE `' + model + '` SET `status` = \'' + _constants.STATUS_INACTIVE + '\' WHERE `status` = \'' + _constants.STATUS_UNPUBLISHED + '\';';
        return queryInterface.sequelize.query(
            sqlRaw
        );
    }).then(function () {
        logger.warn('Remove published/unpublished/republished from status for ' + model);
        return queryInterface.changeColumn(
            model,
            'status',
            {
                type: Sequelize.ENUM(
                    _constants.STATUS_DRAFT,
                    _constants.STATUS_REVIEW,
                    _constants.STATUS_APPROVED,
                    _constants.STATUS_DECLINED,
                    _constants.STATUS_INACTIVE,
                    _constants.STATUS_ACTIVE,
                    _constants.STATUS_DIRTY,
                    _constants.STATUS_ARCHIVED
                ),
                allowNull: true,
                defaultValue: _constants.STATUS_DRAFT
            }
        );
    });
    return result;
}