// Add media columns
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'media',
            'originalFilename',
            {
                type: Sequelize.STRING,
                allowNull: true            }
        ).then(function () {
            return queryInterface.addColumn(
                'media',
                'creatorResourceId',
                {
                    type: Sequelize.STRING,
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'media',
                'createDate',
                {
                    type: Sequelize.DATE,
                    allowNull: false,
                    defaultValue: Sequelize.literal('CURRENT_TIMESTAMP'),
                }
            );
        });

    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'media',
            'originalFilename'
        ).then(function () {
            return queryInterface.removeColumn(
                'media',
                'creatorResourceId'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'media',
                'createDate'
            );
        });
    }
};