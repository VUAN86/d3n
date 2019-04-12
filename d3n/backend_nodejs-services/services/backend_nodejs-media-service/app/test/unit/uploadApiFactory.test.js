var _ = require('lodash');
var async = require('async');
var fs = require('fs');
var gm = require('gm').subClass({imageMagick: true});
var should = require('should');
var path = require('path');
var Config = require('./../../config/config.js');
var UploadApiFactory = require('./../../factories/uploadApiFactory.js');

describe('Low-level image processing', function () {
    it('convertOriginalFileToJpg', function (done) {
        var params = {
            id: 1,
            _fileInfo: {
                path: path.join(__dirname, '../resources/seahorse.png')
            }
        }

        cleanup(Config.httpUpload.tempFolder + '/' + params.id);

        UploadApiFactory.convertOriginalFileToJpg(params, function(err) {
            should.not.exist(err);

            gm(params._originalTempPath).identify(function(err, data) {
                should.not.exist(err);
                data.should.be.an.Object;
                data.should.have.property('format', 'JPEG');
                data.should.have.property('Interlace', 'JPEG');

                cleanup(params._originalTempPath);
                        
                done();
            });
        });
    });
    it('thumbnailsGenerate', function (done) {
        var params = {
            _thumbnails: [
                {
                    width: 200,
                    height: 200,
                    path: path.join(Config.httpUpload.tempFolder, '200x200.jpg')
                }
            ],
            _fileInfo: {
                path: path.join(__dirname, '../resources/test.jpg')
            }
        }

        cleanup(params._thumbnails[0].path);

        UploadApiFactory.thumbnailsGenerate(params, function(err) {
            should.not.exist(err);
            
            gm(params._thumbnails[0].path).identify(function(err, data) {
                should.not.exist(err);
                data.should.be.an.Object;
                data.should.have.property('format', 'JPEG');
                data.should.have.property('Interlace', 'JPEG');
                
                data.size.should.be.an.Object;
                data.size.should.have.property('width');
                data.size.should.have.property('height');
                [data.size.width, data.size.height].should.containEql(200);
                Math.min(data.size.width, data.size.height).should.be.belowOrEqual(200);

                cleanup(params._thumbnails[0].path);
                
                done();
            });
        });
    });
});


describe('Low-level image processing queue', function () {

    it('convertOriginalFileToJpg', function (done) {
        this.timeout(30000);
        var counter = 0;
        var iterations = 2 * Config.mediaConfiguration.imageProcessingQueueSize;
        
        var sourcePath = path.join(__dirname, '../resources/seahorse.png');
        var destPaths = Array.apply(null, Array(iterations)).map(function (value, i) {
            return Config.httpUpload.tempFolder + '/' + (i + 1);
        });

        cleanup(destPaths);

        async.times(iterations, function (i, next) {
            var params = {
                id: i + 1,
                _fileInfo: {
                    path: sourcePath
                }
            };
    
            UploadApiFactory.convertOriginalFileToJpg(params, function(err) {
                if (err) return setImmediate(next, err);
    
                fs.stat(destPaths[i], function(err, stats) {
                    if (stats) counter ++;
                    setImmediate(next);
                });
            });
        }, function (err) {
            should.not.exist(err);
            counter.should.be.eql(iterations);

            cleanup(destPaths);
            
            done();
        });
    });
});

function cleanup(paths) {
    if (!_.isArray(paths)) {
        paths = [paths];
    }

    _.forEach(paths, function(path) {
        try {
            fs.unlinkSync(path);
        } catch (err) {
        }
    });
}
