/*
 * This migration includes the following changes:
 *   7632 - remove of the unique index in tenant configuration
 *   9482 - rename of stats fields on profile
 */
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.removeIndex(
            'tenant', 
            'tenantNameUrl'
        ).then(function (){
            return queryInterface.removeColumn(
                'profile',
                'stat_friendsAccepted'
            );
        }).then(function () {
            return queryInterface.renameColumn(
                'profile', 
                'stat_friendsRejected',
                'stat_friendsBlocked');
        });
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.addIndex(
            'tenant',
            ['name', 'url'],
            {
                indexName: 'tenantNameUrl',
                indicesType: 'UNIQUE'
            }
        ).then(function () {
            return queryInterface.addColumn(
                'profile',
                'stat_friendsAccepted',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.renameColumn(
                'profile',
                'stat_friendsBlocked',
                'stat_friendsRejected'
            );
        });
    }
};
