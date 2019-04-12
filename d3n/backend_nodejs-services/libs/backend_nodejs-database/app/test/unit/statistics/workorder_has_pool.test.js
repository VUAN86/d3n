var should = require('should');
var Util = require('./../../util/statistics.js');
var DataIds = require('./../../config/_id.data.js');
var Workorder = Util.RdbmsModels.Workorder.Workorder;
var WorkorderHasPool = Util.RdbmsModels.Workorder.WorkorderHasPool;
var Pool = Util.RdbmsModels.Question.Pool;

describe('WORKORDER_HAS_POOL', function () {
    it('create bulk', function (done) {
        WorkorderHasPool.bulkCreate([
            { workorderId: DataIds.WORKORDER_1_ID, poolId: DataIds.POOL_2_ID },
            { workorderId: DataIds.WORKORDER_2_ID, poolId: DataIds.POOL_1_ID }
        ]).then(function () {
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Pool.findOne({ where: {
                id: DataIds.POOL_1_ID
            }});
        }).then(function (pool) {
            pool.stat_assignedWorkorders.should.equal(2);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('create single instance', function (done) {
        WorkorderHasPool.create({
            workorderId: DataIds.WORKORDER_2_ID, poolId: DataIds.POOL_1_ID
        }).then(function () {
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Pool.findOne({ where: {
                id: DataIds.POOL_1_ID
            }});
        }).then(function (pool) {
            pool.stat_assignedWorkorders.should.equal(2);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('delete single instance', function (done) {
        WorkorderHasPool.create({
            workorderId: DataIds.WORKORDER_2_ID, poolId: DataIds.POOL_1_ID 
        }).then(function() { 
            return WorkorderHasPool.findOne({ where: {
                workorderId: DataIds.WORKORDER_1_ID, poolId: DataIds.POOL_1_ID
            }});
        }).then(function (workorderPool) {
            return workorderPool.destroy();
        }).then(function () {
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Pool.findOne({ where: {
                id: DataIds.POOL_1_ID
            }});
        }).then(function (pool) {
            pool.stat_assignedWorkorders.should.equal(1);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('delete bulk', function (done) {
        WorkorderHasPool.bulkCreate([
            { workorderId: DataIds.WORKORDER_1_ID, poolId: DataIds.POOL_2_ID },
            { workorderId: DataIds.WORKORDER_2_ID, poolId: DataIds.POOL_1_ID }
        ]).then(function () {
            return WorkorderHasPool.destroy({
                where: { workorderId: DataIds.WORKORDER_1_ID },
                individualHooks: true
            });
        }).then(function () {
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Pool.findOne({ where: {
                id: DataIds.POOL_1_ID
            }});
        }).then(function (pool) {
            pool.stat_assignedWorkorders.should.equal(1);
            done();
        }).catch(function(err) {
            done(err);
        });
    });
});
