var _ = require('lodash');
var async = require('async');
var should = require('should');
var assert = require('chai').assert;
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var ProfileData = require('./../config/profile.data.js');
var SessionData = require('./../config/session.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeGlobalClientSession = KeyvalueService.Models.AerospikeGlobalClientSession;

describe('GlobalClientSession', function () {

    it('test addOrUpdateClientSession', function (done) {

        async.series([
            function (next) {
                AerospikeGlobalClientSession.getUserSession(SessionData.USER_IDS[0], function (err) {
                    try {
                        assert.strictEqual(err, Errors.DatabaseApi.NoRecordFound);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },

            function (next) {
                AerospikeGlobalClientSession.getUserClientSessions(SessionData.USER_IDS[0], function (err, sessions) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(sessions.length, 0);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            // add client session. user session not exists, it will be created
            function (next) {
                AerospikeGlobalClientSession.addOrUpdateClientSession(SessionData.USER_IDS[0], SessionData.CLIENT_SESSIONS[0], function (err) {
                    if (err) {
                        return next(err);
                    }
                    AerospikeGlobalClientSession.getUserClientSessions(SessionData.USER_IDS[0], function (err, sessions) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(sessions.length, 1);
                            assert.deepEqual(sessions[0], SessionData.CLIENT_SESSIONS[0]);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                });
            },
            // add client session
            function (next) {
                AerospikeGlobalClientSession.addOrUpdateClientSession(SessionData.USER_IDS[0], SessionData.CLIENT_SESSIONS[1], function (err) {
                    if (err) {
                        return next(err);
                    }
                    AerospikeGlobalClientSession.getUserClientSessions(SessionData.USER_IDS[0], function (err, sessions) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(sessions.length, 2);
                            assert.deepEqual(sessions[0], SessionData.CLIENT_SESSIONS[0]);
                            assert.deepEqual(sessions[1], SessionData.CLIENT_SESSIONS[1]);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                });
            },

            function (next) {
                AerospikeGlobalClientSession.getClientSession(SessionData.USER_IDS[0], SessionData.CLIENT_SESSIONS[0].clientId, function (err, session) {
                    try {
                        assert.ifError(err);
                        assert.deepEqual(session, SessionData.CLIENT_SESSIONS[0]);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            function (next) {
                AerospikeGlobalClientSession.getClientSession(SessionData.USER_IDS[0], SessionData.CLIENT_SESSIONS[1].clientId, function (err, session) {
                    try {
                        assert.ifError(err);
                        assert.deepEqual(session, SessionData.CLIENT_SESSIONS[1]);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // record not found, clientId not exists
            function (next) {
                AerospikeGlobalClientSession.getClientSession(SessionData.USER_IDS[0], 'nonexistingclientid', function (err, session) {
                    try {
                        assert.isOk(err);
                        assert.deepEqual(err, Errors.DatabaseApi.NoRecordFound);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // record not found, userId not exists
            function (next) {
                AerospikeGlobalClientSession.getClientSession('useridnotexists', 'nonexistingclientid', function (err, session) {
                    try {
                        assert.isOk(err);
                        assert.deepEqual(err, Errors.DatabaseApi.NoRecordFound);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },

            // update a client session
            function (next) {
                var clientSession = _.clone(SessionData.CLIENT_SESSIONS[1]);
                clientSession.gatewayURL += 'new';

                AerospikeGlobalClientSession.addOrUpdateClientSession(SessionData.USER_IDS[0], clientSession, function (err) {
                    if (err) {
                        return next(err);
                    }
                    AerospikeGlobalClientSession.getUserClientSessions(SessionData.USER_IDS[0], function (err, sessions) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(sessions.length, 2);
                            assert.deepEqual(sessions[0], SessionData.CLIENT_SESSIONS[0]);
                            assert.deepEqual(sessions[1], clientSession);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                });
            }




        ], done);

    });

    it('test TTL', function (done) {
        
        var ttl1;
        var ttl2;
        async.series([
            function (next) {
                try {
                    AerospikeGlobalClientSession.removeUserSession(SessionData.USER_IDS[0], next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },

            function (next) {
                AerospikeGlobalClientSession.getUserSession(SessionData.USER_IDS[0], function (err) {
                    try {
                        assert.strictEqual(err, Errors.DatabaseApi.NoRecordFound);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            // add session , ttl updated
            function (next) {
                AerospikeGlobalClientSession.addOrUpdateClientSession(SessionData.USER_IDS[0], SessionData.CLIENT_SESSIONS[0], next);
            },

            function (next) {
                AerospikeGlobalClientSession.getUserSessionWithMetadata(SessionData.USER_IDS[0], function (err, result) {
                    try {
                        assert.ifError(err);
                        var ttl = result.__metadata__.ttl;
                        assert.strictEqual(ttl, AerospikeGlobalClientSession._ttl);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // add a new session , ttl updated
            function (next) {
                // wait a bit so ttl will decrease
                setTimeout(function () {
                    AerospikeGlobalClientSession.addOrUpdateClientSession(SessionData.USER_IDS[0], SessionData.CLIENT_SESSIONS[2], next);
                }, 1500);
            },
            
            function (next) {
                AerospikeGlobalClientSession.getUserSessionWithMetadata(SessionData.USER_IDS[0], function (err, result) {
                    try {
                        assert.ifError(err);
                        var ttl = result.__metadata__.ttl;
                        assert.strictEqual(ttl, AerospikeGlobalClientSession._ttl);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },

        ], done);

    });
    it('test removeClientSession', function (done) {

        async.series([
            function (next) {
                AerospikeGlobalClientSession.getUserSession(SessionData.USER_IDS[1], function (err) {
                    try {
                        assert.strictEqual(err, Errors.DatabaseApi.NoRecordFound);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },

            function (next) {
                AerospikeGlobalClientSession.getUserClientSessions(SessionData.USER_IDS[1], function (err, sessions) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(sessions.length, 0);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            // add client session. user session not exists, it will be created automatically
            function (next) {
                AerospikeGlobalClientSession.addOrUpdateClientSession(SessionData.USER_IDS[1], SessionData.CLIENT_SESSIONS[0], function (err) {
                    if (err) {
                        return next(err);
                    }
                    AerospikeGlobalClientSession.getUserClientSessions(SessionData.USER_IDS[1], function (err, sessions) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(sessions.length, 1);
                            assert.deepEqual(sessions[0], SessionData.CLIENT_SESSIONS[0]);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                });
            },

            function (next) {
                AerospikeGlobalClientSession.addOrUpdateClientSession(SessionData.USER_IDS[1], SessionData.CLIENT_SESSIONS[1], function (err) {
                    if (err) {
                        return next(err);
                    }
                    AerospikeGlobalClientSession.getUserClientSessions(SessionData.USER_IDS[1], function (err, sessions) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(sessions.length, 2);
                            assert.deepEqual(sessions[0], SessionData.CLIENT_SESSIONS[0]);
                            assert.deepEqual(sessions[1], SessionData.CLIENT_SESSIONS[1]);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                });
            },

            // remove non existing, return error
            function (next) {
                AerospikeGlobalClientSession.removeClientSession(SessionData.USER_IDS[1], {clientId: 'erw34', gatewayURL: 'asdasd'}, function (err) {
                    try {
                        assert.strictEqual(err, Errors.DatabaseApi.NoRecordFound);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },

            function (next) {
                AerospikeGlobalClientSession.removeClientSession(SessionData.USER_IDS[1], SessionData.CLIENT_SESSIONS[0], function (err) {
                    if (err) {
                        return next(err);
                    }

                    AerospikeGlobalClientSession.getUserClientSessions(SessionData.USER_IDS[1], function (err, sessions) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(sessions.length, 1);
                            assert.deepEqual(sessions[0], SessionData.CLIENT_SESSIONS[1]);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });

                });
            },

            function (next) {
                AerospikeGlobalClientSession.removeClientSession(SessionData.USER_IDS[1], SessionData.CLIENT_SESSIONS[1], function (err) {
                    if (err) {
                        return next(err);
                    }

                    AerospikeGlobalClientSession.getUserClientSessions(SessionData.USER_IDS[1], function (err, sessions) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(sessions.length, 0);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });

                });
            },
            
            /*  user session is not removed if no client session exists
            // session is removed completely from aerospike
            function (next) {
                AerospikeGlobalClientSession.getUserSession(SessionData.USER_IDS[1], function (err) {
                    try {
                        assert.strictEqual(err, Errors.DatabaseApi.NoRecordFound);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            }
            */


        ], done);

    });
    
    it('test getTenantId', function (done) {

        async.series([
            // no client session
            function (next) {
                AerospikeGlobalClientSession.getTenantId('notexist', 'notexist', function (err) {
                    try {
                        assert.strictEqual(err, Errors.DatabaseApi.NoRecordFound);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // add client session then get tenantId
            function (next) {
                AerospikeGlobalClientSession.addOrUpdateClientSession(SessionData.USER_IDS[2], SessionData.CLIENT_SESSIONS[3], function (err) {
                    if (err) {
                        return next(err);
                    }
                    AerospikeGlobalClientSession.getTenantId(SessionData.USER_IDS[2], SessionData.CLIENT_SESSIONS[3].clientId, function (err, tenantId) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(SessionData.CLIENT_SESSIONS[3].appConfig.tenantId, tenantId);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                });
            },
            
            // try to get tenantId for non existing client
            function (next) {
                AerospikeGlobalClientSession.getTenantId(SessionData.USER_IDS[2], SessionData.CLIENT_SESSIONS[4].clientId, function (err) {
                    try {
                        assert.strictEqual(err, Errors.DatabaseApi.NoRecordFound);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // add a new client session then get tenantId
            function (next) {
                AerospikeGlobalClientSession.addOrUpdateClientSession(SessionData.USER_IDS[2], SessionData.CLIENT_SESSIONS[4], function (err) {
                    if (err) {
                        return next(err);
                    }
                    AerospikeGlobalClientSession.getTenantId(SessionData.USER_IDS[2], SessionData.CLIENT_SESSIONS[4].clientId, function (err, tenantId) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(SessionData.CLIENT_SESSIONS[4].appConfig.tenantId, tenantId);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                });
            }
            
        ], done);

    });
    
});
