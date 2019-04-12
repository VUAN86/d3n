// Add field paymentStructureId to PaymentResourceBillOfMaterial/PaymentTenantBillOfMaterial
// Execute raw queries due to Sequelize bug https://github.com/sequelize/sequelize/issues/6031
module.exports = {
    up: function (queryInterface, Sequelize) {
        var sqlRaw = 'ALTER TABLE `payment_tenant_bill_of_material`' + 
                     ' ADD `paymentStructureId` INTEGER(11),' + 
                     ' ADD CONSTRAINT `payment_tenant_bill_of_material_paymentStructureId_idx`' +
                     ' FOREIGN KEY (`paymentStructureId`) REFERENCES `payment_structure` (`id`)' +
                     ' ON DELETE NO ACTION ON UPDATE NO ACTION;';
        return queryInterface.sequelize.query(
            sqlRaw
        ).then(function () {
            sqlRaw = 'ALTER TABLE `payment_resource_bill_of_material`' + 
                     ' ADD `paymentStructureId` INTEGER(11),' + 
                     ' ADD CONSTRAINT `payment_resource_bill_of_material_paymentStructureId_idx`' +
                     ' FOREIGN KEY (`paymentStructureId`) REFERENCES `payment_structure` (`id`)' +
                     ' ON DELETE NO ACTION ON UPDATE NO ACTION;';
            return queryInterface.sequelize.query(
                sqlRaw
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'payment_resource_bill_of_material',
            'paymentStructureId'
        ).then(function () {
            return queryInterface.removeColumn(
                'payment_tenant_bill_of_material',
                'paymentStructureId'
            );
        });
    }
};