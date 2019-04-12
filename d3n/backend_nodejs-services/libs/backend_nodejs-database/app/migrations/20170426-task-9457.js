module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'question_translation',
            'questionBlobKeys',
            {
                type: Sequelize.TEXT,
                allowNull: true
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'question_translation',
            'questionBlobKeys'
        );
    }
};