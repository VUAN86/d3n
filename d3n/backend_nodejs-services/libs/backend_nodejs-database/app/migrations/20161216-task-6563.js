// Add translation needed parameter to question
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'question',
            'isTranslationNeeded',
            {
                type: Sequelize.INTEGER(1),
                allowNull: false,
                defaultValue: 0
            }
        );
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'question',
            'isTranslationNeeded'
        );
    }
};
