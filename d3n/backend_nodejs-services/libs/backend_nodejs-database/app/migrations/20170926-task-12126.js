// Refactor winning component MySQL structure
var logger = require('nodejs-logger')();

var _constants = {
    TYPE_CASINO_COMPONENT: 'CASINO_COMPONENT',
    TYPE_WORD_GUESSING_GAME: 'WORD_GUESSING_GAME',
    DEFAULT_ENTRY_FEE_CURRENCY_MONEY : 'MONEY',
    DEFAULT_ENTRY_FEE_CURRENCY_CREDIT : 'CREDIT',
    DEFAULT_ENTRY_FEE_CURRENCY_BONUS : 'BONUS',
    ENTRY_FEE_CURRENCY_MONEY : 'MONEY',
    ENTRY_FEE_CURRENCY_CREDIT : 'CREDIT',
    ENTRY_FEE_CURRENCY_BONUS : 'BONUS',
    STATUS_INACTIVE: 'inactive',
    STATUS_ACTIVE: 'active',
    STATUS_DIRTY: 'dirty',
    STATUS_ARCHIVED: 'archived',
};

module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.dropTable(
            'game_has_winning_component_rule'
        ).then(function () {
            return queryInterface.dropTable(
                'winning_component_super_prize'
            );
        }).then(function () {
            return queryInterface.dropTable(
                'winning_component_rule'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'winning_component',
                'typeOfWinning'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'winning_component',
                'amount'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'winning_component',
                'payoutLevel'
            );
        }).then(function () {
            return queryInterface.addColumn(
                'winning_component',
                'tenantId',
                {
                    type: Sequelize.INTEGER,
                    allowNull: false
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'winning_component',
                'title',
                {
                    type: Sequelize.STRING,
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'winning_component',
                'description',
                {
                    type: Sequelize.TEXT,
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'winning_component',
                'rules',
                {
                    type: Sequelize.TEXT,
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'winning_component',
                'status',
                {
                    type: Sequelize.ENUM(
						_constants.STATUS_INACTIVE,
						_constants.STATUS_ACTIVE,
						_constants.STATUS_DIRTY,
						_constants.STATUS_ARCHIVED
					),
					allowNull: false,
					defaultValue: _constants.STATUS_INACTIVE
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'winning_component',
                'type',
                {
                    type: Sequelize.ENUM(
						_constants.TYPE_CASINO_COMPONENT,
						_constants.TYPE_WORD_GUESSING_GAME
					),
					allowNull: false
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'winning_component',
                'startDate',
                {
                    type: Sequelize.DATE,
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'winning_component',
                'endDate',
                {
                    type: Sequelize.DATE,
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'winning_component',
                'imageId',
                {
                    type: Sequelize.STRING,
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'winning_component',
                'prizeDescription',
                {
                    type: Sequelize.TEXT,
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'winning_component',
                'defaultIsFree',
                {
                    type: Sequelize.INTEGER(1),
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'winning_component',
                'defaultEntryFeeAmount',
                {
                    type: Sequelize.DECIMAL(10, 2),
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'winning_component',
                'defaultEntryFeeCurrency',
                {
                    type: Sequelize.ENUM(
						_constants.DEFAULT_ENTRY_FEE_CURRENCY_MONEY,
						_constants.DEFAULT_ENTRY_FEE_CURRENCY_CREDIT,
						_constants.DEFAULT_ENTRY_FEE_CURRENCY_BONUS
					),
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'winning_component',
                'winningConfiguration',
                {
                    type: Sequelize.TEXT,
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'winning_component',
                'createDate',
                {
                    type: Sequelize.DATE,
                    allowNull: false,
                    defaultValue: Sequelize.NOW
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'winning_component',
                'updateDate',
                {
                    type: Sequelize.DATE,
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'winning_component',
                'version',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 1
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'winning_component',
                'publishedVersion',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'winning_component',
                'publishingDate',
                {
                    type: Sequelize.DATE,
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'game_has_winning_component',
                'type'
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game_has_winning_component',
                'rightAnswerPercentage',
                {
                    type: Sequelize.DECIMAL(5,2),
					allowNull: false,
					defaultValue: '100.00'
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game_has_winning_component',
                'isFree',
                {
                    type: Sequelize.INTEGER(1),
					allowNull: false,
					defaultValue: 1
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game_has_winning_component',
                'entryFeeAmount',
                {
                    type: Sequelize.DECIMAL,
					allowNull: false,
					defaultValue: '0.00'
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game_has_winning_component',
                'entryFeeCurrency',
                {
                    type: Sequelize.ENUM(
						_constants.ENTRY_FEE_CURRENCY_MONEY,
						_constants.ENTRY_FEE_CURRENCY_CREDIT,
						_constants.ENTRY_FEE_CURRENCY_BONUS
					),
					allowNull: false,
					defaultValue: _constants.ENTRY_FEE_CURRENCY_BONUS
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return true;
    }
};