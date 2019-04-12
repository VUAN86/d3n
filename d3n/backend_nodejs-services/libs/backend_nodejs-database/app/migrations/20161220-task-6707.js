// Add profileHasGlobalRole table
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.createTable(
            'winning_component_super_prize',
            {
                id: {
                    type: Sequelize.INTEGER(11),
                    primaryKey: true,
                    autoIncrement: true,
                },
                superPrizeId: {
                    type: Sequelize.STRING,
                    allowNull: false,
                    comment: 'Unique ID of the super prize (must match the ID of winning prize in aerospike)',
                    unique: 'winningSuperPrizeId'
                },
                winningAmount: {
                    type: Sequelize.DECIMAL,
                    allowNull: true,
                    comment: 'Amount that can be won'
                },
                insuranceUrl: {
                    type: Sequelize.STRING,
                    allowNull: true,
                    comment: 'Insurance URL to be invoked, may contain placeholders {userId} and {random}'
                },
                randomRangeFrom: {
                    type: Sequelize.INTEGER(11),
                    allowNull: true,
                    comment: 'Random number range bottom bound (inclusive)'
                },
                randomRangeTo: {
                    type: Sequelize.INTEGER(11),
                    allowNull: true,
                    comment: 'Random number range top bound (inclusive)'
                }
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.dropTable(
            'winning_component_super_prize'
        );
    }
};