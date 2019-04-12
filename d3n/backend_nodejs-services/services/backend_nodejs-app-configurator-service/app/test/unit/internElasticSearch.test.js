var should = require('should');
var ElasticSearch = require('./../../services/elasticService.js');
var Config = require('./../../config/config.js');
describe('Intern Elastic Search', function () {
    this.timeout(20000);
    
    it('SUCCESS: healthCheck', function (done) {
        ElasticSearch.checkHealth(function(err, resp) {
            should(err).be.undefined();
            should.exist(resp);
            should(resp).has.property('active_shards');
            should(resp.status).be.not.equal("red");
            done();
        });
    });

    it('SUCCESS: createIndex', function (done) {
        ElasticSearch.createIndex("test",null,function(err, resp) {
            should(err).be.undefined();
            should.exist(resp);
            should(resp).has.property('acknowledged', true);
            done();
        });
    });
    it('SUCCESS: createIndexIfNotExist', function (done) {
        ElasticSearch.createIndexIfNotExist("test",null,function(err, resp) {
            should(err).be.undefined();
            should.exist(resp);
            should(resp).has.property('error');
            should(resp.error).has.property('type', 'index_already_exists_exception');
            done();
        });
    });
    it('SUCCESS: addUpdateDocument Add', function (done) {
        ElasticSearch.addUpdateDocument("test", "test", "1", {name: "Michael", points: 200, address: "Bad Nauheim, Germany"} ,function(err, resp) {
            should(err).be.undefined();
            should.exist(resp);
            should(resp).has.property('_version', 1);
            should(resp).has.property('created', true);
            done();
        });
    });
    it('SUCCESS: addUpdateDocument Update', function (done) {
        ElasticSearch.addUpdateDocument("test", "test", "1", {name: "Michael Kling", points: 200, address: "Bad Nauheim, Germany"} ,function(err, resp) {
            should(err).be.undefined();
            should.exist(resp);
            should(resp).has.property('_version', 2);
            should(resp).has.property('created', false);
            done();
        });
    });
    it('SUCCESS: deleteDocument Existing', function (done) {
        ElasticSearch.deleteDocument("test", "test", "1" ,function(err, resp) {
            should(err).be.undefined();
            should.exist(resp);
            should(resp).has.property('_id', "1");
            should(resp).has.property('found', true);
            done();
        });
    });
    it('SUCCESS: deleteDocument Non Existing', function (done) {
        ElasticSearch.deleteDocument("test", "test", "1" ,function(err, resp) {
            should.exist(err);
            should(err).has.property('message', "Not Found");
            should(err).has.property('statusCode', 404);
            should.exist(resp);
            should(resp).has.property('_id', "1");
            should(resp).has.property('found', false);
            done();
        });
    });
    it('SUCCESS: deleteDocumentIfExists Non Existing', function (done) {
        ElasticSearch.deleteDocumentIfExists("test", "test", "1" ,function(err, resp) {
            should(err).be.undefined();
            should.exist(resp);
            should(resp).has.property('_id', "1");
            should(resp).has.property('found', false);
            done();
        });
    });
    it('SUCCESS: addUpdateBulk', function (done) {
        var bulk = ElasticSearch.createBulk("test", "test");
        bulk.push("1", {name: "Michael Kling", points: 100, address: "Bad Nauheim, Germany"});
        bulk.push("2", {name: "Johannes Kling", points: 200, address: "Bad Nauheim, Germany"});
        bulk.push("3", {name: "Christine Kling", points: 400, address: "Rockenberg, Germany"});
        bulk.push("4", {name: "Johannes Kling", points: 300, address: "Rockenberg, Germany"});

        ElasticSearch.addUpdateBulk(bulk, function(err, resp) {
            should(err).be.undefined();
            should.exist(resp);
            should(resp).has.property('errors', false);
            should(resp).has.property('items').which.has.length(4);
            done();
        });
    });
    it('SUCCESS: wait for search', function (done) {
        setTimeout(function() {
            done();
        }, 9800);
    });
    it('SUCCESS: search', function (done) {
        ElasticSearch.search("test", "test", { match: { name: "Johannes" } }, false, function(err, resp) {
            should(err).be.undefined();
            should.exist(resp);
            should(resp).has.property('hits');
            should(resp.hits).has.property('hits').which.has.length(2);
            done();
        });
    });
    it('SUCCESS: deleteIndex', function (done) {
        ElasticSearch.deleteIndex("test",function(err, resp) {
            should(err).be.undefined();
            should.exist(resp);
            should(resp).has.property('acknowledged', true);
            done();
        });
    });
});