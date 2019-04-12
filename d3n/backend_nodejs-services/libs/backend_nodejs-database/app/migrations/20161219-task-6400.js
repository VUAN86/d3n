// Question: add priority field; add republished value for status field
module.exports = {
    up: function (queryInterface, Sequelize) {
        var _constants = {
            PRIORITY_HIGH: 'high',
            
            STATUS_DRAFT: 'draft',
            STATUS_REVIEW: 'review',
            STATUS_APPROVED: 'approved',
            STATUS_DECLINED: 'declined',
            STATUS_UNPUBLISHED: 'unpublished',
            STATUS_PUBLISHED: 'published',
            STATUS_ARCHIVED: 'archived',
            STATUS_REPUBLISHED: 'republished'
        };
        return queryInterface.addColumn(
            'question',
            'priority',
            {
                type: Sequelize.ENUM(
                    _constants.PRIORITY_HIGH
                ),
                allowNull: true,
                defaultValue: null
            }
        ).then(function () {
            return queryInterface.changeColumn(
                'question',
                'status',
                {
                    type: Sequelize.ENUM(
                        _constants.STATUS_DRAFT,
                        _constants.STATUS_REVIEW,
                        _constants.STATUS_APPROVED,
                        _constants.STATUS_DECLINED,
                        _constants.STATUS_UNPUBLISHED,
                        _constants.STATUS_PUBLISHED,
                        _constants.STATUS_ARCHIVED,
                        _constants.STATUS_REPUBLISHED
                        
                    ),
                    allowNull: false,
                    defaultValue: _constants.STATUS_DRAFT
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        var _constants = {
            STATUS_DRAFT: 'draft',
            STATUS_REVIEW: 'review',
            STATUS_APPROVED: 'approved',
            STATUS_DECLINED: 'declined',
            STATUS_UNPUBLISHED: 'unpublished',
            STATUS_PUBLISHED: 'published',
            STATUS_ARCHIVED: 'archived'
        };
        return queryInterface.removeColumn(
            'question',
            'priority'
        ).then(function () {
            return queryInterface.changeColumn(
                'question',
                'status',
                {
                    type: Sequelize.ENUM(
                        _constants.STATUS_DRAFT,
                        _constants.STATUS_REVIEW,
                        _constants.STATUS_APPROVED,
                        _constants.STATUS_DECLINED,
                        _constants.STATUS_UNPUBLISHED,
                        _constants.STATUS_PUBLISHED,
                        _constants.STATUS_ARCHIVED
                        
                    ),
                    allowNull: false,
                    defaultValue: _constants.STATUS_DRAFT
                }
            );
        });
    }
};