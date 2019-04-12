var KeyvalueService = require('nodejs-aerospike').getInstance().KeyvalueService;
var AerospikeUserToken = KeyvalueService.Models.AerospikeUserToken;
var AerospikeGlobalClientSession = KeyvalueService.Models.AerospikeGlobalClientSession;

function Aerospike(config) {
    this._config = config;
    
    this._data = {};
};

var o = Aerospike.prototype;

o.getUserAuthToken = function (userId, cb) {
    try {
        AerospikeUserToken.findOne({userId: userId}, function (err, res) {
            try {
                if (err) {
                    if (err === 'ERR_ENTRY_NOT_FOUND') {
                        return cb(false, undefined);
                    } else {
                        return cb(err);
                    }
                }

                return cb(err, res.token);
            } catch (e) {
                return cb(e);
            }
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

o.addOrUpdateGlobalClientSession = function (userId, clientSession, cb) {
    AerospikeGlobalClientSession.addOrUpdateClientSession(userId, clientSession, cb);
};

o.removeGlobalClientSesion = function (userId, clientSession, cb) {
    AerospikeGlobalClientSession.removeClientSession(userId, clientSession, function (err) {
        if (err) {
            if (err === 'ERR_ENTRY_NOT_FOUND') {
                return cb();
            } else {
                return cb(err);
            }
        }
        
        return cb();
    });
};

module.exports = Aerospike;
