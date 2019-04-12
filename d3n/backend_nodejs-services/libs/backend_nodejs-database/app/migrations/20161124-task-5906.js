// Add optional fields to advertisement: startDate, endDate, title, text
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'advertisement',
            'startDate',
            {
                type: Sequelize.DATE,
                allowNull: true
            }
        ).then(function () {
            return queryInterface.addColumn(
                'advertisement',
                'endDate',
                {
                    type: Sequelize.DATE,
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'advertisement',
                'title',
                {
                    type: Sequelize.STRING,
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'advertisement',
                'text',
                {
                    type: Sequelize.STRING,
                    allowNull: true
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'advertisement',
            'startDate'
        ).then(function () {
            return queryInterface.removeColumn(
                'advertisement',
                'endDate'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'advertisement',
                'title'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'advertisement',
                'text'
            );
        });
    }
};