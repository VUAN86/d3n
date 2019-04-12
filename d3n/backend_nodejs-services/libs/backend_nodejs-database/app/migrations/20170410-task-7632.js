// Add constraint for unique name and url in case of tenant

// Making the data unique before runing the migration
// UPDATE tenant SET url = CONCAT(url, '/', id);
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addIndex(
            'tenant',
            ['name', 'url'],
            {
                indexName: 'tenantNameUrl',
                indicesType: 'UNIQUE'
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeIndex('tenant', 'tenantNameUrl');
    }
};