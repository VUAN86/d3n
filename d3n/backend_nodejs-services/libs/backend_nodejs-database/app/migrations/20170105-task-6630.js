// Add field workorderId to PaymentResourceBillOfMaterial
// Execute raw queries due to Sequelize bug https://github.com/sequelize/sequelize/issues/6031
module.exports = {
    up: function (queryInterface, Sequelize) {
        var sqlRaw = 'ALTER TABLE `payment_resource_bill_of_material`' + 
                     ' ADD `workorderId` INTEGER(11),' + 
                     ' ADD CONSTRAINT `payment_resource_bill_of_material_workorderId_idx`' +
                     ' FOREIGN KEY (`workorderId`) REFERENCES `workorder` (`id`)' +
                     ' ON DELETE NO ACTION ON UPDATE NO ACTION;';
        return queryInterface.sequelize.query(
            sqlRaw
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'payment_resource_bill_of_material',
            'workorderId'
        );
    }
};