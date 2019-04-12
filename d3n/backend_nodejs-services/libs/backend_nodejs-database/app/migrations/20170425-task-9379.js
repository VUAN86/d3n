// add publishIdx field
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'question',
            'updaterResourceId',
            {
                type: Sequelize.STRING,
                allowNull: true
            }
        ).then(function () {
            queryInterface.addColumn(
                'question_translation',
                'updaterResourceId',
                {
                    type: Sequelize.STRING,
                    allowNull: true
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn('question', 'updaterResourceId').then(function () {
            queryInterface.removeColumn('question_translation', 'updaterResourceId');
        });
    }
};