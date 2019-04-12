// Add parent to gameModule item
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'game_module',
            'key',
            {
                type: Sequelize.STRING,
                allowNull: true
            }
        ).then(function () {
            var sqlRaw = 'ALTER TABLE `game_module`' + 
                         ' ADD `parentId` INTEGER(11),' + 
                         ' ADD CONSTRAINT `game_module_parentId_idx`' +
                         ' FOREIGN KEY (`parentId`) REFERENCES `game_module` (`id`)' +
                         ' ON DELETE NO ACTION ON UPDATE NO ACTION;';
            return queryInterface.sequelize.query(sqlRaw);
        });
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'game_module',
            'key'
        ).then(function () {
            return queryInterface.removeColumn(
                'game_module',
                'parentId'
            );
        });
    }
};
