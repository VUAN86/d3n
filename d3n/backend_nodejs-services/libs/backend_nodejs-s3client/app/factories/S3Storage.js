var _ = require('lodash');
var AWS = require('aws-sdk');
var uuid = require('uuid');
var fs = require('fs');

function S3Storage (config) {
    this.config = config;

    AWS.config.update({
        accessKeyId:        this.config.apiKey, 
        secretAccessKey:    this.config.apiSecret, 
        signatureVersion:   this.config.signatureVersion,
        region:             this.config.region
    });
};

var o = S3Storage.prototype;

o.getObjectAsBuffer = function (args, cb) {
    try {
        var self = this,
            objectId = args.objectId,
            s3 = new AWS.S3()
        ;

        // red file from S3
        s3.getObject({
            Bucket: self.config.bucket,
            Key: objectId
        }, function (err, data) {
            try {
                if(err) {
                    return cb(err);
                }

                // save to outFile
                cb(null, data.Body);
            } catch (e) {
                return cb(e);
            }
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

o.getObject = function (args, cb) {
    try {
        var self = this,
            objectId = args.objectId,
            outFile = args.outFile,
            s3 = new AWS.S3()
        ;

        // read file from S3
        s3.getObject({
            Bucket: self.config.bucket,
            Key: objectId
        }, function (err, data) {
            try {
                if(err) {
                    return cb(err);
                }

                // save to outFile
                fs.writeFile(outFile, data.Body, cb);
            } catch (e) {
                return cb(e);
            }
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

o.uploadObject = function (objectId, objectStream, cbProgress, cbDone) {
    try {
        var self = this;
        self.createBucketIfNotExists(self.config.bucket, function (err) {
            try {
                if(err) {
                    return cbDone(err);
                }
                var s3 = new AWS.S3();

                s3
                .upload({
                    Body: objectStream,
                    Bucket: self.config.bucket,
                    Key: objectId,
                    ACL: 'public-read'
                })
                .on('httpUploadProgress', function(evt) {
                    return cbProgress(evt);
                })
                .send(function(err, data) {
                    return cbDone(err, data);
                });
            } catch (e) {
                return cbDone(e);
            }
        });
    } catch (e) {
        return setImmediate(cbDone, e);
    }
};

o.deleteObjects = function (args, cb) {
    try {
        var self = this,
            objectKeys = args.objectKeys,
            items = [],
            s3 = new AWS.S3()
        ;
        for(var i=0; i<objectKeys.length; i++) {
            items.push({
                Key: objectKeys[i]
            });
        }

        s3.deleteObjects({
            Bucket: self.config.bucket,
            Delete: {
                Objects: items
            }
        }, cb);
    } catch (e) {
        return setImmediate(cb, e);
    }
};

o.listBuckets = function (cb) {
    var s3 = new AWS.S3();
    s3.listBuckets(cb);
};

o.listObjects = function (params, cb) {
  var self = this,
      s3 = new AWS.S3(),
      listParams = _.clone(params);

    if (!_.has(listParams, 'Bucket')) {
      listParams.Bucket = self.config.bucket;
    }

    if (!_.has(listParams, 'MaxKeys')) {
      listParams.MaxKeys = 100;
    }

    s3.listObjectsV2(listParams, cb);
};

o.createBucketIfNotExists = function (bucketName, cb) {
    try {
        var s3 = new AWS.S3();
        s3.listBuckets(function(err, data) {
            try {
                if (err) {
                    return cb(err);
                }

                for (var index in data.Buckets) {
                    if(data.Buckets[index].Name === bucketName) { // already exists, just return
                        return cb(false);
                    }
                }

                // create bucket
                s3.createBucket({
                    Bucket: bucketName
                }, cb);

            } catch (e) {
                return cb(e);
            }
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

module.exports = S3Storage;
