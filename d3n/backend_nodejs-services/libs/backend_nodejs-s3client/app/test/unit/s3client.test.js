var _ = require('lodash');
var fs = require('fs');
var should = require('should');
var assert = require('chai').assert;
var S3Client = require('./../../../index.js');
var Storage = S3Client.getInstance(_.merge(S3Client.Config, {amazon_s3: {bucket: 'f4m-test'}})).Storage;

function deleteRequireCache() {
    var keys = _.keys(require.cache);
    for(var i=0; i<keys.length; i++) {
        delete require.cache[keys[i]];
    }
};

describe('Amazon S3 Storage', function () {
    this.timeout(10000);
    describe('Upload object', function () {

        it('upload OK', function (done) {
            Storage.uploadObject('testKey.jpg',
                fs.createReadStream('./app/test/unit/test.jpg'),
                function (evt) {
                },
                function(err, data) {
                    if (err) return done(err);
                    data.Key.should.be.eql('testKey.jpg');
                    done();
                });
        });
    });

    describe('List objects', function () {
        it('listing OK', function (done) {
            Storage.listObjects({'Prefix': 'testKey.jpg'}, function(err, list) {
                if (err) return done(err);
                list.Contents.length.should.be.eql(1);
                list.Contents[0].Key.should.be.eql('testKey.jpg');
                list.Contents[0].Size.should.be.eql(27791);
                done();
            })
        });
    });

    describe('Delete object', function () {
        it('delete OK', function (done) {
            Storage.deleteObjects({objectKeys: ['testKey.jpg']}, function(err, list) {
                if (err) return done(err);
                list.Deleted.length.should.be.eql(1);
                list.Deleted[0].Key.should.be.eql('testKey.jpg');
                done();
            })
        });
    });
    
    describe('Buckets', function () {
        it('buckets OK', function () {
            
            // test prefixes
            var prefixes = ['f4m-', 'f4m-dev-', 'f4m-staging-', 'f4m-nightly-'];
            for(var i=0; i<prefixes.length; i++) {
                var prefix = prefixes[i];
                deleteRequireCache();
                process.env.S3_API_BUCKET_PREFIX = prefix;
                S3Client = require('./../../../index.js');
                
                for(var k in S3Client.Config.buckets) {
                    var cat = S3Client.Config.buckets[k];
                    for(var k2 in cat) {
                        var bucketName = cat[k2];
                        assert.isTrue(_.startsWith(bucketName, prefix));
                    }
                }
            }
            
            // by default f4m-dev- prefix
            deleteRequireCache();
            delete process.env.S3_API_BUCKET_PREFIX;
            S3Client = require('./../../../index.js');

            for(var k in S3Client.Config.buckets) {
                var cat = S3Client.Config.buckets[k];
                for(var k2 in cat) {
                    var bucketName = cat[k2];
                    assert.isTrue(_.startsWith(bucketName, 'f4m-dev-'));
                }
            }
            
        });
    });
});
