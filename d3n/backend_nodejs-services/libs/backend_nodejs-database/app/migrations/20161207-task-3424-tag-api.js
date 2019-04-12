// add tenantId field for tag model
module.exports = {
    up: function (queryInterface, Sequelize) {
        var sqlRaw = 'ALTER TABLE `tag`' + 
                     ' ADD `tenantId` INTEGER(11),' + 
                     ' ADD CONSTRAINT `tag_tenantId_idx`' +
                     ' FOREIGN KEY (`tenantId`) REFERENCES `tenant` (`id`)' +
                     ' ON DELETE NO ACTION ON UPDATE NO ACTION;';
        return queryInterface.sequelize.query(sqlRaw);
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'tag',
            'tenantId'
        );
    }
};