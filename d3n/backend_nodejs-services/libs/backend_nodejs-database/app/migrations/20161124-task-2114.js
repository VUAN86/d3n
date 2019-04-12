// added coulumns publishIdx, publishMultiIndex and changed column content type
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'question_translation',
            'content',
            {
                type: Sequelize.TEXT,
                allowNull: true
            }
        ).then(function () {
            return queryInterface.addColumn(
                'question_translation',
                'publishIdx',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'question_translation',
                'publishMultiIndex',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: true
                }
            );
        });
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'question_translation',
            'content',
            {
                type: 'BLOB',
                allowNull: true
            }
        ).then(function () { 
            return queryInterface.removeColumn('question_translation', 'publishIdx'); 
        }).then(function () {
            return queryInterface.removeColumn('question_translation', 'publishMultiIndex');
        });
    }
};
