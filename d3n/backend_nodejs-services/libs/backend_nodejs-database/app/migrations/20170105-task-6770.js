// Add payUnit, paymentFor to paymentStructure
module.exports = {
    up: function (queryInterface, Sequelize) {
        var _constants = {
            PAYMENT_FOR_PER_ACCEPTED_QUESTION: 'per_accepted_question',
            PAYMENT_FOR_PER_REVIEWED_QUESTION: 'per_reviewed_question'
        };
        return queryInterface.addColumn(
            'payment_structure',
            'payUnit',
            {
                type: Sequelize.INTEGER(11),
                allowNull: false,
                defaultValue: 0
            }
        ).then(function () {
            return queryInterface.addColumn(
                'payment_structure',
                'paymentFor',
                {
                    type: Sequelize.ENUM(
                        _constants.PAYMENT_FOR_PER_ACCEPTED_QUESTION,
                        _constants.PAYMENT_FOR_PER_REVIEWED_QUESTION
                    ),
                    allowNull: false
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'payment_structure',
            'payUnit'
        ).then(function () {
            return queryInterface.removeColumn(
                'payment_structure',
                'paymentFor'
            );
        });
    }
};