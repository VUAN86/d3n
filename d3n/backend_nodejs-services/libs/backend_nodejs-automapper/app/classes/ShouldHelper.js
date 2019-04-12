var _ = require('lodash');

function ShouldHelper(config) {
    var self = this;
    self._config = config;
}

ShouldHelper.getInstance = function (config) {
    return new ShouldHelper(config);
};

ShouldHelper.prototype.treatAsList = function (array, offset, limit) {
    var self = this;
    var items = array;
    if (_.isArray(array)) {
        items = array.sort();
    } else {
        items = _.toArray(array).sort();
    }
    _.forEach(items, function (item, key) {
        items[key] = self.isoDates(item);
    });
    var result = { items: items, total: items.length };
    result.offset = _.isUndefined(offset) ? 0 : offset;
    result.limit = _.isUndefined(limit) ? self._config.limit : limit;
    return result;
};

ShouldHelper.prototype.isoDates = function (request, response) {
    if (!_.isObject(request)) {
        return request;
    }
    _.forEach(_.keys(request), function (key, value) {
        if (key.endsWith('Date') && request[key]) {
            // remove milliseconds
            var date = request[key].toString();
            if (date.length > 19) {
                date = date.substring(0, date.length - 4) + '000Z';
                // Match date format of response (date or date-time)
                if (_.has(response, key) && response[key]) {
                    request[key] = date.substring(0, response[key].toString().length);
                } else {
                    request[key] = date;
                }
            }
        }
    });
    return request;
};

ShouldHelper.prototype.deepEqual = function (requestContent, responseContent, exceptFields) {
    function except(source, exceptFields) {
        if (exceptFields && _.isArray(exceptFields)) {
            _.forEach(exceptFields, function (field) {
                if (_.has(source, field)) {
                    delete source[field];
                }
            });
        }
        // also skip null/undefined fields
        _.forEach(_.keys(source), function (field) {
            if (_.isUndefined(source[field]) || _.isNull(source[field])) {
                delete source[field];
            }
        });
        return source;
    }
    var self = this;
    var should = require('should');
    should.exist(requestContent);
    should.exist(responseContent);
    var reqContent = self.isoDates(requestContent, responseContent);
    reqContent = except(_.cloneDeep(reqContent), exceptFields);
    var resContent = except(_.cloneDeep(responseContent), exceptFields);
    should(reqContent).be.deepEqual(resContent);
    return true;
};

ShouldHelper.prototype.deepEqualList = function (requestContent, responseContent, exceptFields) {
    var self = this;
    if (_.isUndefined(requestContent) && _.isUndefined(responseContent)) {
        return true;
    }
    var should = require('should');
    should(_.isArray(requestContent)).be.true;
    should(_.isArray(responseContent)).be.true;
    should(requestContent.length).be.equal(responseContent.length);
    _.forEach(requestContent, function (value, key) {
        self.deepEqual(requestContent[key], responseContent[key], exceptFields);
    });
};

module.exports = ShouldHelper;