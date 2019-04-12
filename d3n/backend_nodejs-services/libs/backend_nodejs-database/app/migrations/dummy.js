/*
 * CLEANUP:
 *
 * This file serves no practical purpose, can be removed so make sure when you
 * delete here you also delete it from _migrations_ table from all environments
 * (DEV/STAGING/NIGHTLY)
 */

var Bluebird = require('bluebird');

module.exports = {
    up: function () {
        // Return a succesfully resolved promise
        return new Bluebird(function (resolve, reject) { return resolve(); });
    },

    down: function () {
        return new Bluebird(function (resolve, reject) { return resolve(); });
    }
};