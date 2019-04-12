// Add isEscalated flag
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'question',
            'isEscalated',
            {
                type: Sequelize.INTEGER(1),
                allowNull: true
            }
        ).then(function () {
            return queryInterface.addColumn(
                'question_translation',
                'isEscalated',
                {
                    type: Sequelize.INTEGER(1),
                    allowNull: true
                }
            );
        });     
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn('question', 'isEscalated').then(function () {
            queryInterface.removeColumn('question_translation', 'isEscalated');
        });
    }
};