
var _ = require('lodash');
var async = require('async');
var should = require('should');
var request = require('supertest');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./../config/_id.data.js');
var Data = require('./../config/profile.data.js');
var DataLoader = require('./../config/index.js');
var Service = require('./../../../index.js');

var HttpHelper = function (series) {
    this._service = new Service();
    this._series = series ? series : ['direct', 'gateway'];
    this.sinon = require('sinon');
};

module.exports = HttpHelper;

HttpHelper.prototype.series = function () {
    return this._series;
};

HttpHelper.prototype.getService = function () {
    return this._service;
};

HttpHelper.prototype.build = function (done) {
    done();
};

HttpHelper.prototype.shutdown = function (done) {
    done();
}

/**
 * Builds basic auth header payload as required by specs.
 * 
 * @param {string} username
 * @param {string} password
 * @returns {string} The basic http auth header payload
 */
HttpHelper.prototype.buildAuthHeader = function(username, password) {
    var token = username + ":" + password;
    buffer = new Buffer(username + ":" + password);

    return "Basic " + buffer.toString('base64');
}

HttpHelper.prototype.getSuccess = function (url, queryParams, contentCallback, doneCallback) {
    var self = this;

    try {
        var apiReq = request(self._service.httpProvider())
            .get(url)
            .query(queryParams)
            .set('Authorization', self.buildAuthHeader('admin', 's3cr3t'));

        apiReq.expect(200)
            .expect('Content-Type', /json/)
            .end(function (err, res) {
                if (err) {
                    return doneCallback(err);
                }
                if (contentCallback) {
                    contentCallback(res.body.content);
                }

                return doneCallback();
            });
    } catch (ex) {
        return doneCallback(ex);
    }
};

HttpHelper.prototype.getError = function (url, code, error, doneCallback) {
    var self = this;

    try {
        var apiReq = request(self._service.httpProvider()).get(url)
                .set('Authorization', self.buildAuthHeader('admin', 's3cr3t'));

        apiReq.expect(code)
            .expect('Content-Type', /json/)
            .end(function (err, res) {
                should.deepEqual(res.body.error, error);
                if (err) {
                    return doneCallback(err);
                }
                return doneCallback();
            });
    } catch (ex) {
        return doneCallback(ex);
    }
};

HttpHelper.prototype.postSuccess = function (url, postParams, contentCallback, doneCallback) {
    var self = this;

    try {
        var apiReq = request(self._service.httpProvider())
            .post(url)
            .set('Authorization', self.buildAuthHeader('admin', 's3cr3t'))
            .send(postParams);

        apiReq.expect(200)
            .expect('Content-Type', /json/)
            .end(function (err, res) {
                if (err) {
                    return doneCallback(err);
                }
                if (contentCallback) {
                    contentCallback(res.body.content);
                }

                return doneCallback();
            });
    } catch (ex) {
        return doneCallback(ex);
    }
};

HttpHelper.prototype.postError = function (url, postParams, code, error, doneCallback) {
    var self = this;

    try {
        var apiReq = request(self._service.httpProvider()).post(url)
                .set('Authorization', self.buildAuthHeader('admin', 's3cr3t'))
                .send(postParams);

        apiReq.expect(code)
            .expect('Content-Type', /json/)
            .end(function (err, res) {
                should.deepEqual(res.body.error, error);
                if (err) {
                    return doneCallback(err);
                }
                return doneCallback();
            });
    } catch (ex) {
        return doneCallback(ex);
    }
};


HttpHelper.prototype.securityCheck = function (url, headers, httpStatusCodeResponse, contentCallback, doneCallback) {
    var self = this;

    try {
        var apiReq = request(self._service.httpProvider()).get(url);
        if (!_.isEmpty(headers)) {
            _.forEach(headers, function(value, key) {
                apiReq.set(key, value);
            })
        }

        apiReq.expect(httpStatusCodeResponse)
            .end(function (err, res) {
                if (err) {
                    return doneCallback(err);
                }

                if (contentCallback) {
                    contentCallback(res.text);
                }

                return doneCallback();
            });
    } catch (ex) {
        return doneCallback(ex);
    }
};

HttpHelper.prototype.securityIpCheck = function (url, httpStatusCodeResponse, contentCallback, doneCallback) {
    var self = this;

    try {
        var backupClientIpConfig;
        app = self._service.httpProvider();
        backupClientIpConfig = app.locals.tenantManagementConfiguration.clientIP;

        app.locals.tenantManagementConfiguration.clientIP = ['1.2.3.4'];

        var apiReq = request(self._service.httpProvider()).get(url)
                        .set('Authorization', self.buildAuthHeader('admin', 's3cr3t'));

        apiReq.expect(httpStatusCodeResponse)
            .end(function (err, res) {
                app.locals.tenantManagementConfiguration.clientIP = backupClientIpConfig;
                if (err) {
                    return doneCallback(err);
                }
                if (contentCallback) {
                    contentCallback(res.text);
                }
                return doneCallback();
            });
    } catch (ex) {
        return doneCallback(ex);
    }
};

