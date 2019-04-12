// Add field to Game: instantAnswerFeedbackDelay
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'game',
            'instantAnswerFeedbackDelay',
            {
                type: Sequelize.INTEGER(11),
                allowNull: false,
                defaultValue: 0
            }
        );
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'game',
            'instantAnswerFeedbackDelay'
        );
    }
};