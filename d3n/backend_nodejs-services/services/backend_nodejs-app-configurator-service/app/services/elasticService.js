/**
 * Created by Michael on 30.12.2016.
 */
var elasticsearch=require('elasticsearch');
var Config = require('./../config/config.js');
var logger = require('nodejs-logger')();

var client = null;
/**
 * Lazy loading elastic search client
 * @returns {*}
 */
var getClient = function() {
    if (!client) {
        client = new elasticsearch.Client( {
            hosts: [
                'http://'+
                (Config.elastic.username?Config.elastic.username+":":"")+
                (Config.elastic.password?Config.elastic.password+"@":"")+
                Config.elastic.server+
                (Config.elastic.port?":"+Config.elastic.port:"")+
                '/'
            ]
        });
    }
    return client;
};


module.exports = {
    /**
     * Creates index in elastic search instance
     * @param name Name of index
     * @param settings Settings of the index (object including otional mappings and settings object)
     * @param callback(err, resp)
     */
    _createIndex: function (name, settings, callback) {
        var createObject = {
            index: name,
            body: {}
        };
        if (settings && settings.settings) {
            createObject["body"]["settings"] = settings.settings;
        }
        if (settings && settings.mappings) {
            createObject["body"]["mappings"] = settings.mappings;
        }
        return getClient().indices.create(createObject, callback);
    },

    /**
     * Creates index in elastic search instance
     * @param name Name of index
     * @param settings Settings of the index (object including otional mappings and settings object)
     * @param callback(err, resp)
     */
    createIndex: function(name, settings, callback) {
        return this._createIndex(name, settings, function (err, resp, status) {
            if (err) {
                logger.error('Elastic Search Create Index', err, resp, status);
            }
            return callback(err, resp);
        });
    },
    /**
     * Creates index in elastic search instance, does not return error, if index already exists
     * @param name Name of index
     * @param settings Settings of the index (object including otional mappings and settings object)
     * @param callback(err, resp)
     */
    createIndexIfNotExist: function(name, settings, callback) {
        return this._createIndex(name, settings, function (err, resp, status) {
            if (err && typeof err === "object" && err.message && err.message.indexOf("index_already_exists_exception") !== -1) {
                //All ok, go on.
                logger.debug('Create Index failure is ok - createIndexIfNotExist was called');
                err = undefined;
            }
            if (err) {
                logger.error('Elastic Search Create Index if Not Exist', err, resp, status);
            }
            return callback(err, resp);
        });
    },
    /**
     * Removes index
     * @param name Name of index
     * @param callback(err, resp)
     */
    deleteIndex: function(name, callback) {
        return getClient().indices.delete({
            index: name
        },function(err,resp,status) {
            if (err) {
                logger.error('Elastic Search Delete Index', err, resp, status);
            }
            return callback(err,resp);
        });
    },
    /**
     * Get health status of elastic search instance.
     * @param callback(err, resp)
     */
    checkHealth: function(callback) {
        return getClient().cluster.health({},function(err,resp,status) {
            if (err) {
                logger.error('Elastic Search Health Check', err, resp, status);
            }
            return callback(err, resp);
        });
    },
    
    getConnectionStatus: function (cb) {
        try {
            if (client === null) {
                return setImmediate(cb, false, 'NS');
            }

            client.ping(function (err) {
                return cb(false, err ? 'NOK' : 'OK');
            });
        } catch (e) {
            return setImmediate(cb, e);
        }
    },
    
    /**
     * Adds or updates a document into elastic search
     * @param index Index to be used
     * @param type Type of document
     * @param id Id of the document
     * @param document Document contents
     * @param callback
     */
    addUpdateDocument: function(index, type, id, document, callback) {
        return getClient().index({
            index: index,
            id: id,
            type: type,
            body: document
        },function(err,resp,status) {
            if (err) {
                logger.error('Elastic Search Add/Update Document', err, resp, status);
            }
            return callback(err, resp);
        });
    },
    /**
     * Creates bulk object which can be used to add/update a bulk of entries.
     * Push all entries via push method into the bulk object and use addUpdateBulk
     * @param index Index to be used
     * @param type Type of document
     * @param conversionFunction Optional function which will convert the inserted element. Example: function(document){ document.search = document.firstName+focument.lastName; return document; }
     * @returns {{index: *, type: *, bulk: Array, push: push}}
     */
    createBulk: function(index, type, conversionFunction) {
        return {
            index: index,
            type: type,
            bulk: [],
            conversionFunction: conversionFunction,
            push: function(id, document) {
                if (this.conversionFunction) {
                    document = this.conversionFunction(document);
                }
                this.bulk.push(
                    { index: { _index: this.index, _type: this.type, _id: id } },
                    document
                );
            }
        };
    },

    /**
     * Adds or updates a bulk of documents into elastic search
     * Use createBulk in order to create a bulk object
     * @param bulk {{index: *, type: *, bulk: Array, push: push}}
     * @param callback
     */
    addUpdateBulk: function(bulk, callback) {
        return getClient().bulk({
            maxRetries: 5,
            index: bulk.index,
            type: bulk.index,
            body: bulk.bulk
        },function(err,resp,status) {
            if (err) {
                logger.error('Elastic Search Add/Update Bulk', err, resp, status);
            }
            return callback(err, resp);
        });
    },
    /**
     * Deletes a document from elastic search
     * @param index Index to be used
     * @param type Type of document
     * @param id Id of the document
     * @param callback
     */
    _deleteDocument: function (index, type, id, callback) {
        return getClient().delete({
            index: index,
            id: id,
            type: type
        }, callback);
    },
    /**
     * Deletes a document from elastic search
     * @param index Index to be used
     * @param type Type of document
     * @param id Id of the document
     * @param callback
     */
    deleteDocument: function (index, type, id, callback) {
        return this._deleteDocument(index, type, id, function (err, resp, status) {
            if (err) {
                logger.error('Elastic Search Add/Update Document', err, resp, status);
            }
            return callback(err, resp);
        });
    },
    /**
     * Deletes a document from elastic search, ignores not found error
     * @param index Index to be used
     * @param type Type of document
     * @param id Id of the document
     * @param callback
     */
    deleteDocumentIfExists: function(index, type, id, callback) {
        return this._deleteDocument(index, type, id, function (err, resp, status) {
            if (err && typeof err === "object" && err.message && err.message.indexOf("Not Found") !== -1) {
                //All ok, go on.
                err = undefined;
            }
            if (err) {
                logger.error('Elastic Search Create Index if Not Exist', err, resp, status);
            }
            return callback(err, resp);
        });
    },
    /**
     * Adds or updates a document into elastic search
     * @param index Index to be used
     * @param type Type of document
     * @param query A ElasticSearch Query (For example: match: { "name" : "Kling" } )
     * @param sort Sort object or false
     * @param callback
     */
    search: function(index, type, query, sort, callback) {
        if (!sort) sort = {};
        return getClient().search({
            index: index,
            type: type,
            fields: ["name"],
            body: {
                query: query,
                sort: sort
            }
        },function(err,resp,status) {
            if (err) {
                logger.error('Elastic Search Search', err, resp, status);
            }
            return callback(err, resp);
        });
    }
};
