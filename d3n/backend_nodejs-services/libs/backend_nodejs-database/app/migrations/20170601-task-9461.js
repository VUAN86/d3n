// Drop constraint for questionOrTranslationEventLog.resourceId, rename to profilePhoto
module.exports = {
    up: function (queryInterface, Sequelize) {
        var sqlRaw ="ALTER TABLE question_or_translation_event_log DROP FOREIGN KEY question_or_translation_event_log_ibfk_2;";
        return queryInterface.sequelize.query(sqlRaw).catch(function (err) {});
    },
    
    down: function (queryInterface, Sequelize) {
        return true;
    }
};