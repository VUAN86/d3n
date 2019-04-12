// add tenantId field for language model
module.exports = {
    up: function (queryInterface, Sequelize) {
        var sql1 = 'ALTER TABLE `question_has_tag` DROP FOREIGN KEY `question_has_tag_ibfk_2`';
        var sql2 = 'ALTER TABLE `question_has_tag` ADD CONSTRAINT `question_has_tag_ibfk_2` FOREIGN KEY (`tagTag`) REFERENCES `tag` (`tag`) ON DELETE NO ACTION ON UPDATE CASCADE';
        
        
        var sql3 = 'ALTER TABLE `pool_has_tag` DROP FOREIGN KEY `pool_has_tag_ibfk_2`';
        var sql4 = 'ALTER TABLE `pool_has_tag` ADD CONSTRAINT `pool_has_tag_ibfk_2` FOREIGN KEY (`tagTag`) REFERENCES `tag` (`tag`) ON DELETE NO ACTION ON UPDATE CASCADE';
        
        var sql5 = 'ALTER TABLE `media_has_tag` DROP FOREIGN KEY `media_has_tag_ibfk_2`';
        var sql6 = 'ALTER TABLE `media_has_tag` ADD CONSTRAINT `media_has_tag_ibfk_2` FOREIGN KEY (`tagTag`) REFERENCES `tag` (`tag`) ON DELETE NO ACTION ON UPDATE CASCADE';
        queryInterface.sequelize
        .query(sql1)
        .then(function () {
            return queryInterface.sequelize.query(sql2);
        })
        .then(function () {
            return queryInterface.sequelize.query(sql3);
        })
        .then(function () {
            return queryInterface.sequelize.query(sql4);
        })
        .then(function () {
            return queryInterface.sequelize.query(sql5);
        })
        .then(function () {
            return queryInterface.sequelize.query(sql6);
        });
    },
    
    down: function (queryInterface, Sequelize) {
        var sql1 = 'ALTER TABLE `question_has_tag` DROP FOREIGN KEY `question_has_tag_ibfk_2`';
        var sql2 = 'ALTER TABLE `question_has_tag` ADD CONSTRAINT `question_has_tag_ibfk_2` FOREIGN KEY (`tagTag`) REFERENCES `tag` (`tag`) ON DELETE NO ACTION ON UPDATE NO ACTION';
        
        
        var sql3 = 'ALTER TABLE `pool_has_tag` DROP FOREIGN KEY `pool_has_tag_ibfk_2`';
        var sql4 = 'ALTER TABLE `pool_has_tag` ADD CONSTRAINT `pool_has_tag_ibfk_2` FOREIGN KEY (`tagTag`) REFERENCES `tag` (`tag`) ON DELETE NO ACTION ON UPDATE NO ACTION';
        
        var sql5 = 'ALTER TABLE `media_has_tag` DROP FOREIGN KEY `media_has_tag_ibfk_2`';
        var sql6 = 'ALTER TABLE `media_has_tag` ADD CONSTRAINT `media_has_tag_ibfk_2` FOREIGN KEY (`tagTag`) REFERENCES `tag` (`tag`) ON DELETE NO ACTION ON UPDATE NO ACTION';
        queryInterface.sequelize
        .query(sql1)
        .then(function () {
            return queryInterface.sequelize.query(sql2);
        })
        .then(function () {
            return queryInterface.sequelize.query(sql3);
        })
        .then(function () {
            return queryInterface.sequelize.query(sql4);
        })
        .then(function () {
            return queryInterface.sequelize.query(sql5);
        })
        .then(function () {
            return queryInterface.sequelize.query(sql6);
        });
    }
};