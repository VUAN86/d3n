// allow null for questionId and questionTranslationId of paymentAction
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'payment_action',
            'questionId',
            {
                type: Sequelize.INTEGER(11),
                allowNull: true
            }
        ).then(function () {
            return queryInterface.changeColumn(
                'payment_action',
                'questionTranslationId',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: true
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'payment_action',
            'questionId',
            {
                type: Sequelize.INTEGER(11),
                allowNull: false
            }
        ).then(function () {
            return queryInterface.changeColumn(
                'payment_action',
                'questionTranslationId',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false
                }
            );
        });
    }
};