// Add values to Voucher.category
var _constants = {
    STATUS_DRAFT: 'draft',
    STATUS_REVIEW: 'review',
    STATUS_APPROVED: 'approved',
    STATUS_DECLINED: 'declined',
    STATUS_INACTIVE: 'inactive',
    STATUS_ACTIVE: 'active',
    STATUS_DIRTY: 'dirty',
    STATUS_ARCHIVED: 'archived',
    STATUS_AUTOMATIC_TRANSLATION: 'automatic_translation'
};

module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'question_translation',
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
                    _constants.STATUS_AUTOMATIC_TRANSLATION
                ),
                allowNull: false,
                defaultValue: _constants.STATUS_DRAFT
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'question_translation',
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
                allowNull: false,
                defaultValue: _constants.STATUS_DRAFT
            }
        );
    }
};