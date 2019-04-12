var _constants = {
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('profileHasStats', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true,
        },
        profileId: {
            type: DataTypes.STRING,
            allowNull: false,
            comment: "The id of the user in the aerospike database",
            unique: "profileTenantStats"
        },
        tenantId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            comment: "The tenant identifier, the stats applies to a certain tenant only",
            unique: "profileTenantStats"
        },
        // Statistics
        stat_handicap: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_appsInstalled: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        }, 
        stat_friendsInvited: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_friendsBlocked: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_gamesPlayed: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_gamesInvitedFromFriends: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_gamesFriendsInvitedToo: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_gamesPlayedWithFriends: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_gamesPlayedWithPublic: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_gamesWon: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_gamesLost: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_gamesDrawn: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_rightAnswers: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_wrongAnswers: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_averageAnswerSpeed: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_skippedQuestions: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_adsViewed: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_paidWinningComponentsPlayed: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_freeWinningComponentsPlayed: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_skippedWinningComponents: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_voucherWon: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_superPrizesWon: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_bonusPointsWon: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_creditsWon: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_moneyPlayed: {
            type: DataTypes.DECIMAL(10,2),
            allowNull: false,
            defaultValue: '0.00'
        },
        stat_moneyWon: {
            type: DataTypes.DECIMAL(10,2),
            allowNull: false,
            defaultValue: '0.00'
        },
        stat_totalCreditsPurchased: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_totalMoneyCharged: {
            type: DataTypes.DECIMAL(10,2),
            allowNull: false,
            defaultValue: '0.00'
        },
        stat_questionsCreated: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_questionsReviewed: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_questionsTranslationsCreated: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_questionsTranslationsReviewed: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_questionsQuality: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_favoriteGame: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_issuesReported: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        
    },
    {
        timestamps: false,
        tableName: 'profile_has_stats',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.profile, { foreignKey: 'profileId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.tenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
