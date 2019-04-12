// Add block attributes to QuestionTranslation
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'question_translation',
            'blockerResourceId',
            {
                type: Sequelize.STRING,
                allowNull: true
            }
        ).then(function () {
            return queryInterface.addColumn(
                'question_translation',
                'blockDate',
                {
                    type: Sequelize.DATE,
                    allowNull: true
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'question_translation',
            'blockerResourceId'
        ).then(function () {
            return queryInterface.removeColumn(
                'question_translation',
                'blockDate'
            );
        });
    }
};