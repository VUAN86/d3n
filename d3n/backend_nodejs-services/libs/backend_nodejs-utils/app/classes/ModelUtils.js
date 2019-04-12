var _ = require('lodash');
var fs = require('fs');
var path = require('path');
var logger = require('nodejs-logger')();

module.exports = {
    walkKeyvalue: function (modelRoot) {
        return _walk(modelRoot, function (model) {
            return model.startsWith('aerospike');
        });
    },

    walkRdbms: function (modelRoot) {
        return _walk(modelRoot, function (model) {
            return !_.startsWith('aerospike', model);
        });
    },
    
    beforeUpdate: function (model, options) {
        if (process.env.DATABASE_LOGGER === 'true') {
            logger.info('beforeUpdate model<-fields', options.model.name, JSON.stringify(options.fields));
        }
        model.version += 1;
        if (_.includes(options.fields, 'status')) {
            if (model.status === options.model.constants().STATUS_ACTIVE) {
                model.publishedVersion = model.version;
                model.publishingDate = _.now();
            } else if (model.status === options.model.constants().STATUS_INACTIVE) {
                model.publishedVersion = null;
                model.publishingDate = null;
            }
        } else {
            if (model.status === options.model.constants().STATUS_ACTIVE) {
                model.status = options.model.constants().STATUS_DIRTY;
            }
        }
    },
};

function _walk(modelRoot, filterCallback) {
    var results = [];
    var models = fs.readdirSync(modelRoot).filter(filterCallback);
    models.forEach(function (model) {
        var file = modelRoot + '/' + model;
        var stat = fs.statSync(file);
        if (stat && stat.isDirectory()) {
            results = results.concat(_walk(file, filterCallback));
        }
        else {
            var interface = modelRoot.match(/([^\/]*)\/*$/)[0];
            results.push({
                interface: interface,
                model: path.basename(model, '.js'),
                definition: file
            });
        }
    })
    return results;
}
