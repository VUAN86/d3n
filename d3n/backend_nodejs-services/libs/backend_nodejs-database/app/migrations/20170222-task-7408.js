// remove PaymentStructure.paymentFor, ApplicationHasGame.status
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'payment_structure',
            'paymentFor'
        ).then(function () {
            return queryInterface.removeColumn(
                'application_has_game',
                'status'
            );
        });
    },

    down: function (queryInterface, Sequelize) {
        var _constants = {
            STATUS_ACTIVE: 'active',
            STATUS_INACTIVE: 'inactive',
            STATUS_ARCHIVED: 'archived',
            PAYMENT_FOR_PER_ACCEPTED_QUESTION: 'per_accepted_question',
            PAYMENT_FOR_PER_REVIEWED_QUESTION: 'per_reviewed_question'
        };
        return queryInterface.addColumn(
            'payment_structure',
            'paymentFor',
            {
                type: Sequelize.ENUM(
                    _constants.PAYMENT_FOR_PER_ACCEPTED_QUESTION,
                    _constants.PAYMENT_FOR_PER_REVIEWED_QUESTION
                ),
                allowNull: true
            }
        ).then(function () {
            return queryInterface.addColumn(
                'application_has_game',
                'status',
                {
                    type: Sequelize.ENUM(
                        _constants.STATUS_ACTIVE,
                        _constants.STATUS_INACTIVE,
                        _constants.STATUS_ARCHIVED
                    ),
                    allowNull: true,
                    defaultValue: _constants.STATUS_ACTIVE
                }
            );
        });
    }
};
