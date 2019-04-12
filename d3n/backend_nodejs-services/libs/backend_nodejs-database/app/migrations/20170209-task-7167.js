// Add imageId to Game
module.exports = {
    up: function (queryInterface, Sequelize) {
        var sqlRaw = 'ALTER TABLE `game`' + 
                     ' ADD `imageId` VARCHAR(255),' + 
                     ' ADD CONSTRAINT `game_imageId_idx`' +
                     ' FOREIGN KEY (`imageId`) REFERENCES `media` (`id`)' +
                     ' ON DELETE NO ACTION ON UPDATE NO ACTION;';
        return queryInterface.sequelize.query(
            sqlRaw
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'game',
            'imageId'
        );
    }
};