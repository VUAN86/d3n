var async = require('async');
var assert = require('chai').assert;
var _ = require('lodash');
var sinon = require('sinon');
var sinonSandbox;
var StatisticsProviders = require('../../../index.js');
var Database = require('nodejs-database');
var Aerospike = require('nodejs-aerospike');
var AerospikeConnection = Aerospike.getInstance().KeyvalueService.Models.AerospikeConnection;

describe('STATISTICS PROVIDERS', function () {
    describe('DatabasesStatisticsProvider', function () {
        afterEach(function () {
            if (sinonSandbox) {
                sinonSandbox.restore();
            }
        });
        it('generateStatistics() ok', function (done) {
            var mysqlInstance = Database.getInstance();
            //var aerospikeInstance = Aerospike.getInstance();
            
            var dbStatistics = new StatisticsProviders['databasesStatisticsProvider']({
                mysqlInstance: mysqlInstance,
                aerospikeConnectionModel: AerospikeConnection
            });
            
            async.series([
                // aerospike connection not started
                function (next) {
                    dbStatistics.generateStatistics(function (err, res) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(res.connectionsToDb.mysql, 'OK');
                            assert.strictEqual(res.connectionsToDb.aerospike, 'NS');
                            assert.strictEqual(res.connectionsToDb.spark, 'NA');
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                },
                
                // connect to aerospike
                function (next) {
                    AerospikeConnection.connect(function (err) {
                        try {
                            assert.ifError(err);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                },
                
                // aerospike connected
                function (next) {
                    dbStatistics.generateStatistics(function (err, res) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(res.connectionsToDb.mysql, 'OK');
                            assert.strictEqual(res.connectionsToDb.aerospike, 'OK');
                            assert.strictEqual(res.connectionsToDb.spark, 'NA');
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                }
            ], done);
        });
        
        /*
        it('generateStatistics() not ok', function (done) {
            
            delete Database._instance;
            delete Aerospike._instance;
            
            var mysqlInstance = Database.getInstance({
                rdbms: {
                    host: '111.222.333.444',
                    port: '3306',
                    schema: 'f4m',
                    username: 'root',
                    password: 'masterkey',
                    force: true
                }
            });
            var aerospikeInstance = Aerospike.getInstance({
                keyvalue: {
                    hosts: '111.222.333.444:3000',
                    user: null,
                    password: null,
                    metadata: {
                        ttl: 0, // default TTL
                        gen: 0
                    },
                    limit: 20,
                    log: {
                        level: 1,
                        file: undefined
                    },
                    policies: {
                        timeout: 1000
                    }
                }
            });
            
            var dbStatistics = new StatisticsProviders['databasesStatisticsProvider']({
                mysqlInstance: mysqlInstance,
                aerospikeInstance: aerospikeInstance
            });
            
            async.series([
                // aerospike connection not started
                
                function (next) {
                    dbStatistics.generateStatistics(function (err, res) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(res.connectionsToDb.aerospike, 'NS');
                            assert.strictEqual(res.connectionsToDb.spark, 'NA');
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                },
                
                // connect to aerospike
                function (next) {
                    aerospikeInstance.KeyvalueService.connect(function (c) {
                        try {
                            assert.isNull(c);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                },
                
                function (next) {
                    dbStatistics.generateStatistics(function (err, res) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(res.connectionsToDb.aerospike, 'NOK');
                            assert.strictEqual(res.connectionsToDb.spark, 'NA');
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                },
            ], done);
        });
        */
        
    });
    
    
});
