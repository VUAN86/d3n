var _ = require('lodash');
var ip2loc = require("ip2location-nodejs");

function GeoLocation(config) {
    this._config = config || {};
    
    var dbLocation = this._config.dbLocation || __dirname + '/../config/ip2location.bin';
    
    ip2loc.IP2Location_init(dbLocation);
};

var o = GeoLocation.prototype;

o.getCountryCode = function (ip) {
    var code = ip2loc.IP2Location_get_country_short(ip);
    return code.length === 2 ? code : null;
};
module.exports = GeoLocation;
