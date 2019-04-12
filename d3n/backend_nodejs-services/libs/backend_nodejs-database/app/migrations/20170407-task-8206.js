// Drop constraint for Profile.profilePhotoId, rename to profilePhoto
module.exports = {
    up: function (queryInterface, Sequelize) {
        var sqlRaw ="ALTER TABLE profile DROP FOREIGN KEY profile_profilePhotoId_idx;";
        return queryInterface.sequelize.query(sqlRaw).catch(function (err) {});
    },
    
    down: function (queryInterface, Sequelize) {
        return true;
    }
};