var _ = require('lodash');
var fs = require('fs');
var path = require('path');

var statisticsPath = path.join(__dirname, 'statistics');

var Statistics = (function() {
    var statistics = {};
    var statisticFileNames = fs.readdirSync(statisticsPath);
    statisticFileNames.forEach(function (statisticFileName) {
        var query = fs.readFileSync(path.join(statisticsPath, statisticFileName), 'utf8');
        var entityNames = getEntityNames(statisticFileName);

        var updatedEntity = getUpdatedEntity(statistics, entityNames.from);
        updatedEntity[entityNames.to] = query;
    });
    return statistics;
})();

module.exports = Statistics;

function getEntityNames(fileName) {
    var tokens = fileName.split('.');
    var entityNames = {
        to: tokens[1],
        from: tokens[0]
    };
    return entityNames;
}

function getUpdatedEntity(statistics, name) {
    if (!_.has(statistics, name)) {
        statistics[name] = {};
    }
    return statistics[name];
}