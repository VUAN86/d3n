// Add isActivatedManually flags
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'question',
            'isActivatedManually',
            {
                type: Sequelize.INTEGER(1),
                allowNull: true
            }
        ).then(function () {
            return queryInterface.addColumn(
                'question_translation',
                'isActivatedManually',
                {
                    type: Sequelize.INTEGER(1),
                    allowNull: true
                }
            );
        });     
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn('question', 'isActivatedManually').then(function () {
            queryInterface.removeColumn('question_translation', 'isActivatedManually');
        });
    }
};