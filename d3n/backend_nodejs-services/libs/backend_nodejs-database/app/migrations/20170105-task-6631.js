// Add totalTransactions:integer to paymentResourceBillOfMaterial, paymentTenantBillOfMaterial
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'payment_structure',
            'payment_resource_bill_of_material',
            {
                type: Sequelize.INTEGER(11),
                allowNull: true
            }
        ).then(function () {
            return queryInterface.addColumn(
                'payment_structure',
                'payment_tenant_bill_of_material',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: true
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'payment_structure',
            'payment_resource_bill_of_material'
        ).then(function () {
            return queryInterface.removeColumn(
                'payment_structure',
                'payment_tenant_bill_of_material'
            );
        });
    }
};