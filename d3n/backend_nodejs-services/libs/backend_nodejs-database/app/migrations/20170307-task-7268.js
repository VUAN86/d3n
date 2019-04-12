// Add values to Voucher.category
var _constants = {
    CATEGORY_FASHION: 'Fashion',
    CATEGORY_TRAVEL: 'Travel',
    CATEGORY_GOING_OUT: 'Going Out',
    CATEGORY_HOME_AND_GARDEN: 'Home and Garden',
    CATEGORY_GIFT_AND_FLOWERS: 'Gifts & Flowers',
    CATEGORY_LIFESTYLE: 'Lifestyle',
    CATEGORY_CAR_AND_MOTIVE: 'Car & Motive'
};

module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'voucher',
            'category',
            {
                type: Sequelize.ENUM(
                    _constants.CATEGORY_FASHION,
                    _constants.CATEGORY_TRAVEL,
                    _constants.CATEGORY_GOING_OUT,
                    _constants.CATEGORY_HOME_AND_GARDEN,
                    _constants.CATEGORY_GIFT_AND_FLOWERS,
                    _constants.CATEGORY_LIFESTYLE,
                    _constants.CATEGORY_CAR_AND_MOTIVE
                ),
                allowNull: true
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'voucher',
            'category',
            {
                type: Sequelize.ENUM(
                    _constants.CATEGORY_FASHION,
                    _constants.CATEGORY_TRAVEL,
                    _constants.CATEGORY_GOING_OUT,
                    _constants.CATEGORY_HOME_AND_GARDEN,
                    _constants.CATEGORY_GIFT_AND_FLOWERS
                ),
                allowNull: true
            }
        );
    }
};