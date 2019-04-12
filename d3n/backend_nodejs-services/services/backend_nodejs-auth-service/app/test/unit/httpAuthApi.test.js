//var async = require('async'),
//    uuid = require('node-uuid'),
//    should = require('should'),
//    request = require('supertest'),
//    Data = require('./../config/profile.data.js'),
//    AuthService = require('./../../../index.js'),
//    Errors = require('./../../config/errors.js'),
//    JwtUtils = require('./../../utils/jwtUtils.js'),
//    HttpApi = require('./../../api/httpApi.js'),
//    ProtocolMessage = require('nodejs-protocol');

//var authService = new AuthService();

//describe('HTTP API', function () {
//    before(function (done) {
//        authService.build(function (err) {
//            if (err) {
//                return done(err);
//            }
//            done();
//        });
//    });
//    beforeEach(function (done) {
//        Data.load(function (err, res) {
//            if (err) {
//                return done(err);
//            }
//            done();
//        });
//    });
//    after(function (done) {
//        authService.shutdown(function (err) {
//            if (err) {
//                return done(err);
//            }
//            done();
//        });
//    });
    
//    describe('POST /register', function () {
//        it('Local: HTTP 200', function (done) {
//            _apiPostSucc('register', Data.TEST_USER, function (content) {
//                should.exists(content);
//                content.should.have.property('userid').which.is.a.String();
//            }, done);
//        });
//        it('Local: HTTP 401 ERR_AUTH_VALIDATION_FAILED', function (done) {
//            _apiPostFail('register', {
//                salt: Data.TEST_USER.salt
//            }, Errors.AuthApi.ValidationFailed, done);
//        });
//        //if (process.env.FACEBOOK_TOKEN && process.env.FACEBOOK_PROFILE) {
//        //    it('Facebook: HTTP 200', function (done) {
//        //        _apiPostSucc('register', {
//        //            facebook: Data.FACEBOOK_USER.facebook
//        //        }, function (content) {
//        //            should.exists(content);
//        //            content.should.have.property('userid').which.is.a.String();
//        //        }, done);
//        //    });
//        //    it('Facebook: HTTP 401 ERR_AUTH_PROVIDER_TOKEN_NOT_VALID', function (done) {
//        //        _apiPostFail('register', {
//        //            facebook: 'invalid_token'
//        //        }, Errors.AuthApi.ProviderTokenNotValid, done);
//        //    });
//        //}
//        //if (process.env.GOOGLE_TOKEN && process.env.GOOGLE_PROFILE) {
//        //    it('Google: HTTP 200', function (done) {
//        //        _apiPostSucc('register', {
//        //            google: Data.GOOGLE_USER.google
//        //        }, function (content) {
//        //            should.exists(content);
//        //            content.should.have.property('userid').which.is.a.String();
//        //        }, done);
//        //    });
//        //    it('Google: HTTP 401 ERR_AUTH_PROVIDER_TOKEN_NOT_VALID', function (done) {
//        //        _apiPostFail('register', {
//        //            google: 'invalid_token'
//        //        }, Errors.AuthApi.ProviderTokenNotValid, done);
//        //    });
//        //}
//    });

//    describe('POST /auth', function () {
//        it('Local: HTTP 200', function (done) {
//            _apiPostSucc('auth', {
//                userid: Data.LOCAL_USER.id,
//                pass: JwtUtils.md5(Data.LOCAL_USER.salt)
//            }, function (content) {
//                should.exists(content);
//                content.should.have.property('token').which.is.a.String();
//            }, done);
//        });
//        it('Local: HTTP 401 ERR_DATABASE_USER_ID_NOT_FOUND', function (done) {
//            _apiPostFail('auth', {
//                userid: Data.TEST_USER.id,
//                pass: JwtUtils.md5(Data.LOCAL_USER.salt)
//            }, Errors.DatabaseApi.UserIdNotFound, done);
//        });
//        it('Local: HTTP 401 ERR_DATABASE_PASSWORD_WRONG', function (done) {
//            _apiPostFail('auth', {
//                userid: Data.LOCAL_USER.id,
//                pass: JwtUtils.md5(Data.TEST_USER.salt)
//            }, Errors.DatabaseApi.UserPasswordIsWrong, done);
//        });
//        //if (process.env.FACEBOOK_TOKEN && process.env.FACEBOOK_PROFILE) {
//        //    it('Facebook: HTTP 200', function (done) {
//        //        _apiPostSucc('auth', {
//        //            userid: Data.FACEBOOK_USER.id,
//        //            facebook: Data.FACEBOOK_USER.facebook
//        //        }, function (content) {
//        //            should.exists(content);
//        //            content.should.have.property('token').which.is.a.String();
//        //        }, done);
//        //    });
//        //    it('Facebook: HTTP 401 ERR_DATABASE_USER_ID_NOT_FOUND', function (done) {
//        //        _apiPostFail('auth', {
//        //            userid: Data.TEST_USER.id,
//        //            facebook: Data.FACEBOOK_USER.facebook
//        //        }, Errors.DatabaseApi.UserIdNotFound, done);
//        //    });
//        //    it('Facebook: HTTP 401 ERR_AUTH_PROVIDER_TOKEN_NOT_VALID', function (done) {
//        //        _apiPostFail('auth', {
//        //            userid: Data.FACEBOOK_USER.id,
//        //            facebook: 'invalid_token'
//        //        }, Errors.AuthApi.ProviderTokenNotValid, done);
//        //    });
//        //}
//        //if (process.env.GOOGLE_TOKEN && process.env.GOOGLE_PROFILE) {
//        //    it('Google: HTTP 200', function (done) {
//        //        _apiPostSucc('auth', {
//        //            userid: Data.GOOGLE_USER.id,
//        //            google: Data.GOOGLE_USER.google
//        //        }, function (content) {
//        //            should.exists(content);
//        //            content.should.have.property('token').which.is.a.String();
//        //        }, done);
//        //    });
//        //    it('Google: HTTP 401 ERR_DATABASE_USER_ID_NOT_FOUND', function (done) {
//        //        _apiPostFail('auth', {
//        //            userid: Data.TEST_USER.id,
//        //            google: Data.GOOGLE_USER.google
//        //        }, Errors.DatabaseApi.UserIdNotFound, done);
//        //    });
//        //    it('Google: HTTP 401 ERR_AUTH_PROVIDER_TOKEN_NOT_VALID', function (done) {
//        //        _apiPostFail('auth', {
//        //            userid: Data.GOOGLE_USER.id,
//        //            google: 'invalid_token'
//        //        }, Errors.AuthApi.ProviderTokenNotValid, done);
//        //    });
//        //}
//    });

//    describe('POST /refresh', function () {
//        this.timeout(3000);
//        it('HTTP 200', function (done) {
//            var token = undefined;
//            _apiPostSucc('auth', {
//                userid: Data.LOCAL_USER.id,
//                pass: JwtUtils.md5(Data.LOCAL_USER.salt)
//            }, function (content) {
//                should.exists(content);
//                content.should.have.property('token').which.is.a.String();
//                token = content.token;
//            }, function (err) {
//                should.not.exists(err);
//                _apiPostSucc('refresh', {
//                    token: token
//                }, function (content) {
//                    should.exists(content);
//                    content.should.have.property('token').which.is.a.String();
//                }, done);
//            });
//        });
//        it('HTTP 401 ERR_AUTH_VALIDATION_FAILED', function (done) {
//            var token = undefined;
//            _apiPostSucc('auth', {
//                userid: Data.LOCAL_USER.id,
//                pass: JwtUtils.md5(Data.LOCAL_USER.salt)
//            }, function (content) {
//                should.exists(content);
//                content.should.have.property('token').which.is.a.String();
//                token = content.token;
//            }, function (err) {
//                should.not.exists(err);
//                var message = JwtUtils.decode(token),
//                    expiredToken = JwtUtils.encode({ userid: message.payload.userid, roles: [] }, { expiresIn: 1 });
//                return setTimeout(function () {
//                    _apiPostFail('refresh', {
//                        token: expiredToken
//                    }, Errors.AuthApi.ValidationFailed, done);
//                }, 2000);
//            });
//        });
//    });
    
//    describe('GET /getPublicKey', function () {
//        it('HTTP 200', function (done) {
//            _apiGetSucc('getPublicKey', function (content) { 
//                should.exists(content);
//                content.should.have.property('publicKey').which.is.a.String();
//            }, done);
//        });
//    });

//    describe('POST /changePassword', function () {
//        it('HTTP 200', function (done) {
//            _apiPostSucc('changePassword', {
//                userid: Data.LOCAL_USER.id,
//                oldpass: JwtUtils.md5(Data.LOCAL_USER.salt),
//                newpass: JwtUtils.md5(Data.LOCAL_USER.salt + 'new'),
//                salt: Data.LOCAL_USER.salt
//            }, function (content) {
//                should.not.exists(content);
//            }, function (err) {
//                should.not.exists(err);
//                _apiPostSucc('auth', {
//                    userid: Data.LOCAL_USER.id,
//                    pass: JwtUtils.md5(Data.LOCAL_USER.salt + 'new')
//                }, function (content) {
//                    should.exists(content);
//                    content.should.have.property('token').which.is.a.String();
//                }, done);
//            });
//        });
//        it('HTTP 401 ERR_AUTH_VALIDATION_FAILED [1]', function (done) {
//            _apiPostFail('changePassword', null, Errors.AuthApi.ValidationFailed, done);
//        });
//        it('HTTP 401 ERR_AUTH_VALIDATION_FAILED [2]', function (done) {
//            _apiPostFail('changePassword', {
//                userid: Data.LOCAL_USER.id,
//            }, Errors.AuthApi.ValidationFailed, done);
//        });
//        it('HTTP 401 ERR_AUTH_VALIDATION_FAILED [3]', function (done) {
//            _apiPostFail('changePassword', {
//                userid: Data.LOCAL_USER.id,
//                oldpass: JwtUtils.md5(Data.LOCAL_USER.salt),
//                newpass: JwtUtils.md5(Data.LOCAL_USER.salt + 'new')
//            }, Errors.AuthApi.ValidationFailed, done);
//        });
//        it('HTTP 401 ERR_DATABASE_PASSWORD_WRONG', function (done) {
//            _apiPostFail('changePassword', {
//                userid: Data.LOCAL_USER.id,
//                oldpass: JwtUtils.md5(Data.LOCAL_USER.salt + 'wrong'),
//                newpass: JwtUtils.md5(Data.LOCAL_USER.salt + 'new'),
//                salt: Data.LOCAL_USER.salt
//            }, Errors.DatabaseApi.UserPasswordIsWrong, done);
//        });
//    });
    
//    describe('POST /setUserRole', function () {
//        it('HTTP 200 [1]', function (done) {
//            _apiPostSucc('setUserRole', {
//                userid: Data.LOCAL_USER.id,
//                toadd: ['role4', 'role5', 'role6'],
//                toremove: ['role1', 'role2']
//            }, function (content) {
//                should.not.exists(content);
//            }, done);
//        });
//        it('HTTP 200 [2]', function (done) {
//            _apiPostSucc('setUserRole', {
//                userid: Data.LOCAL_USER.id,
//                toadd: 'role7,role8,role9'
//            }, function (content) {
//                should.not.exists(content);
//            }, done);
//        });
//        it('HTTP 401 ERR_DATABASE_USER_ID_NOT_FOUND', function (done) {
//            _apiPostFail('setUserRole', {
//                userid: Data.TEST_USER.id,
//                toadd: ['role4', 'role5', 'role6'],
//                toremove: ['role1', 'role2']
//            }, Errors.DatabaseApi.UserIdNotFound, done);
//        });
//        it('HTTP 401 ERR_DATABASE_USER_ROLES_NOT_VALID', function (done) {
//            _apiPostFail('setUserRole', {
//                userid: Data.LOCAL_USER.id,
//                toadd: ['role4', 'role5', 'role6'],
//                toremove: ['role4', 'role2']
//            }, Errors.DatabaseApi.UserRolesNotValid, done);
//        });
//    });

//});

//function _apiGetSucc(api, contentCallback, doneCallback) {
//    request(authService.httpProvider())
//            .get('/' + api)
//            .set('Accept', 'application/json')
//            .expect('Content-Type', /application\/json/)
//		    .expect(200)
//		    .end(function (err, res) {
//        if (err) {
//            return doneCallback(err);
//        }
//        var message = new ProtocolMessage(JSON.parse(res.text));
//        //message.isValid('websocketMessage').should.be.eql(true);
//        if (contentCallback) {
//            contentCallback(message.getContent());
//        }
//        doneCallback();
//    });
//}

//function _apiPostSucc(api, data, contentCallback, doneCallback) {
//    try {
//        request(authService.httpProvider())
//            .post('/' + api)
//            .send(data)
//            .set('Accept', 'application/json')
//            .expect('Content-Type', /application\/json/)
//            .expect(200)
//            .end(function (err, res) {
//                if (err) {
//                    return doneCallback(err);
//                }
//                var message = new ProtocolMessage(JSON.parse(res.text));
//                //message.isValid('websocketMessage').should.be.eql(true);
//                if (contentCallback) {
//                    contentCallback(message.getContent());
//                }
//                return doneCallback();
//            });
//    } catch (ex) {
//        return doneCallback(ex);
//    }
//}

//function _apiPostFail(api, data, error, doneCallback) {
//    try {
//        request(authService.httpProvider())
//            .post('/' + api)
//            .send(data)
//            .set('Accept', 'application/json')
//            .expect('Content-Type', /application\/json/)
//            .expect(401)
//            .end(function (err, res) {
//                if (err) {
//                    return doneCallback(err);
//                }
//                var message = new ProtocolMessage(JSON.parse(res.text));
//                //message.isValid('websocketMessage').should.be.eql(true);
//                error.should.be.eql(message.getError());
//                return doneCallback();
//            });
//    } catch (ex) {
//        return doneCallback(ex);
//    }
//}