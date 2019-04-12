// Change QuestionTemplate.help type to TEXT
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'question_template',
            'help',
            {
                type: Sequelize.TEXT,
                allowNull: true
            }
        );
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'question_template',
            'help',
            {
                type: 'BLOB',
                allowNull: true
            }
        );
    }
};
