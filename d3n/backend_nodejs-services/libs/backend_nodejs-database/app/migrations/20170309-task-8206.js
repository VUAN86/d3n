// Drop constraint for Profile.profilePhotoId, rename to profilePhoto
module.exports = {
    up: function (queryInterface, Sequelize) {
        var sqlRaw ="ALTER TABLE profile DROP FOREIGN KEY profile_ibfk_4;";
        return queryInterface.renameColumn('profile', 'profilePhotoId', 'profilePhoto').then(function () {
            return queryInterface.sequelize.query(sqlRaw).catch(function (err) {});
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return true;
    }
};