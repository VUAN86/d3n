var logger = require('nodejs-logger')();

module.exports = {
    EMPTY: "0000-00-00 00:00:00",

    now: function() {
        var date = new Date();
        var month = date.getMonth() + 1;
        var day = date.getDate();
        var hour = date.getHours();
        var min = date.getMinutes();
        var sec = date.getSeconds();
        
        month = (month < 10 ? '0' : '') + month;
        day = (day < 10 ? '0' : '') + day;
        hour = (hour < 10 ? '0' : '') + hour;
        min = (min < 10 ? '0' : '') + min;
        sec = (sec < 10 ? '0' : '') + sec;
    
        var str = date.getFullYear() + '-' + month + '-' + day + ' ' +
            hour + ':' + min + ':' + sec;
        return str;
    },

    isCloseFromNow: function(dateStr, intervalInMinutes) {
        try {
            var milliseconds = intervalInMinutes * 60 * 1000;
            var date = getDateFormString(dateStr);
            var now = Date.now();
            var difference = now - date;

            return difference < milliseconds;
        } catch (ex) {
            logger.error("Shop: error parsing date", ex);
            return false;
        }
    }
}

function getDateFormString(dateStr) {
    var year = dateStr.substring(6, 10);
    var month = dateStr.substring(3, 5) - 1;
    var day = dateStr.substring(0, 2);
    var hour = dateStr.substring(11, 13);
    var min = dateStr.substring(14, 16);
    var sec = dateStr.substring(17, 19);
    
    var date = new Date(year, month, day, hour, min, sec);
    return date.getTime();
}