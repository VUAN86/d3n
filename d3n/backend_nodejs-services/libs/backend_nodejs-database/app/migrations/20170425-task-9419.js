module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'question_or_translation_review',
            'rating',
            {
                type: Sequelize.DECIMAL(2,1).UNSIGNED,
                allowNull: true
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'question_or_translation_review',
            'rating',
            {
                type: Sequelize.INTEGER(1).UNSIGNED,
                allowNull: true
            }
        );
    }
};