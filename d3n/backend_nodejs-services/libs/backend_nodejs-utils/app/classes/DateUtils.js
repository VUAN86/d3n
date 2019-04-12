var _ = require('lodash');

module.exports = {
    isoDate: function (date) {
        var date000Z = (date ? date : _.now()).toString();
        date000Z = date000Z.substring(0, date000Z.length - 3) + '000';
        return (new Date(parseInt(date000Z))).toISOString();
    },

    isoPublish: function (date) {
        if (!date) {
            date = new Date();
        }
        if (!_.isDate(date)) {
            return date;
        }
        var dateZ = date.toISOString().slice(0, 19) + 'Z';
        return dateZ;
    },

    isoDatePublish: function (date) {
        if (!date) {
            date = new Date();
        }
        if (!_.isDate(date)) {
            return date;
        }
        var dateZ = date.toISOString().slice(0, 10) + 'T00:00:00Z';
        return dateZ;
    },

    isoNow: function (date) {
        return this.isoDate(_.now());
    },

    isoPast: function (milliseconds) {
        return this.isoDate(_.now() - (milliseconds ? milliseconds : 1000));
    },

    isoFuture: function (milliseconds) {
        return this.isoDate(_.now() + (milliseconds ? milliseconds : 1000));
    }
};