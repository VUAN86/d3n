// add tenantId field for language model
module.exports = {
    up: function (queryInterface, Sequelize) {
        var sqlRaw = 'ALTER TABLE `question_template`' + 
                     ' ADD `tenantId` INTEGER(11),' + 
                     ' ADD CONSTRAINT `question_template_tenantId_idx`' +
                     ' FOREIGN KEY (`tenantId`) REFERENCES `tenant` (`id`)' +
                     ' ON DELETE NO ACTION ON UPDATE NO ACTION;';
        return queryInterface.sequelize.query(sqlRaw);
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'question_template',
            'tenantId'
        );
    }
};