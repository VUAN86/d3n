// Remove TenantEndConsumerInvoice table
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.dropTable('tenant_end_consumer_invoice');
    },

    down: function (queryInterface, Sequelize) {
        return true;
    }
};