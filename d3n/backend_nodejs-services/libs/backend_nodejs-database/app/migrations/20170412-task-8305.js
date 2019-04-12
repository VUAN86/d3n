// Add QuestionTranslation.publishLanguageIndex
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'question_translation',
            'publishLanguageIndex',
            {
                type: Sequelize.TEXT,
                allowNull: true
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'question_translation',
            'publishLanguageIndex'
        );
    }
};