// Change values for status field of advertisement_provider/voucher_provider: active, inaactive, archived
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'profile',
            'languageId',
            {
                type: Sequelize.INTEGER(11),
                allowNull: true,
            }
        ).then(function () {
            return queryInterface.changeColumn(
                'profile',
                'regionalSettingId',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: true,
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        // Don't revert back or provide removal of entry with empty languageId/regionalSettingId at first
    }
};