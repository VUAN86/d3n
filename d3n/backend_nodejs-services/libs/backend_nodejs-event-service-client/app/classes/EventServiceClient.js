var config = require('../config/config.js');
var errors = require('../config/errors.js');
var _ = require('lodash');
var async  =require('async');
var EventEmitter = require('events').EventEmitter;
var inherits = require('util').inherits;
var logger = require('nodejs-logger')();
var DefaultServiceClient = require('nodejs-default-client');
var RegistryServiceClient = require('nodejs-service-registry-client');
var url = require('url');
var ProtocolMessage = require('nodejs-protocol');


function EventServiceClient (registryServiceURIs, eventServiceName, ownerServiceName) {
    if (!registryServiceURIs) {
        registryServiceURIs = config.registryServiceURIs;
    }
    if (!eventServiceName) {
        eventServiceName = config.eventServiceName;
    }
    
    this._eventServiceName = eventServiceName;
    
    this._defaultClient = new DefaultServiceClient({
        serviceNamespace: this._eventServiceName,
        reconnectForever: true,
        autoConnect: true,
        serviceRegistryClient: new RegistryServiceClient({registryServiceURIs: registryServiceURIs}),
        headers: ownerServiceName ? {service_name: ownerServiceName} : null
    });
    
    // keeps the subscriptionId -> topic association
    this._mapSubscriptionTopic = {};
    
    this._eventServiceURI = null;
    
    this._inited = false;
    
    this._seq = 0;
    
    // call event emiter constructor
    EventEmitter.call(this);
    
    this._setDefaultClientHandlers();
    
    // prevent nodejs crash
    this.on('error', function (error) {
        logger.error('EventServiceClient error event emitted:', error);
    });
    
    
    this._onReconnectedHandlerRunning = false;
    this._onReconnectedToNewInstanceHandlerRunning = false;
};

// inherit from EventEmitter
inherits(EventServiceClient, EventEmitter);

var o = EventServiceClient.prototype;


o._setDefaultClientHandlers = function () {
    var self = this;
    
    self._defaultClient.on('reconnected', self._onReconnectedHandler.bind(self));
    
    self._defaultClient.on('reconnectedToNewInstance', self._onReconnectedToNewInstanceHandler.bind(self));
    
    self._defaultClient.on('message', self._onMessageHandler.bind(self));
};


o._request = function (message, cb, addInFront) {
    try {
        
        var self = this;
        if (_.isFunction(cb)) {
            return self._defaultClient.sendMessage(message, cb, (addInFront === true ? true : false));
        } else {
            return self._defaultClient.sendMessage(message, function (){}, (addInFront === true ? true : false));
        }
        
    } catch (e) {
        return setImmediate(cb, false);
    }
};


/**
 * Subscribe to a topic
 * @param {string} topic
 * @param {callback} cb
 * @returns {unresolved}
 */
o.subscribe = function (topic, cb, addInFront, tryForever) {
    try {
        
        var self = this;
        
        if (!_.isFunction(cb)) {
            logger.warn('publish: callback not provided!');
            return;
        }
        
        if (!_.isString(topic) || !topic.length) {
            return setImmediate(cb, new Error(errors.ERR_INVALID_TOPIC));
        }
        
        if (self._getSubscriptionByTopic(topic) !== null) {
            return setImmediate(cb, new Error(errors.ERR_ALREADY_SUBSCRIBED));
        }
        
        var message = new ProtocolMessage();
        
        message.setMessage(self._eventServiceName + '/subscribe');
        message.setSeq(self._getSeq());
        message.setContent({
            topic: topic
        });
        
        function _subscribe(message, addInFront, cbSubscribe) {
            try {
                self._request(message, function (err, response) {
                    try {
                        if (err) {
                            return cbSubscribe(err, response);
                        }

                        if (response.getError() !== null) {
                            return cbSubscribe(false, response);
                        }

                        var subscription = response.getContent().subscription;
                        self._mapSubscriptionTopic[subscription] = topic;

                        return cbSubscribe(false, response);

                    } catch (e) {
                        logger.error('EventServiceClient register call error', e);
                        return cbSubscribe(e);
                    }
                }, addInFront);
            } catch (e) {
                return setImmediate(cbSubscribe, e);
            }
        };
        
        if (tryForever) {
            if (!_.isPlainObject(tryForever)) {
                tryForever = {};
            }
            var tryForeverTimeout = tryForever.timeout || 10*1000;
            var tryForeverInterval = tryForever.interval || 2*1000;
            async.forever(
                function (next) {
                    logger.debug('EventServiceClient try forever try');
                    async.timeout(_subscribe, tryForeverTimeout)(message, addInFront, function (err, response) {
                        logger.debug('EventServiceClient try forever try response:', err, JSON.stringify(response));
                        if (err) {
                            self._defaultClient.removeMessageCallbacks(message);
                            // retry
                            return setTimeout(next, tryForeverInterval);
                        } else {
                            return next(response);
                        }
                    });
                },
        
                function (response) {
                    if (response && response.getError() !== null) {
                        return cb(new Error(response.getError()), response);
                    }
                    return cb(false, response);
                }
            );
        } else {
            _subscribe(message, addInFront, function (err, response) {
                try {
                    if (err) {
                        return cb(err);
                    }
                    if (response && response.getError() !== null) {
                        return cb(new Error(response.getError()), response);
                    }

                    return cb(false, response);
                } catch (e) {
                    return cb(e);
                }
            });
        }
        
        /*
        self._request(message, function (err, message) {
            try {
                if (err) {
                    return cb(err);
                }
                
                if (message.getError() !== null) {
                    return cb(new Error(message.getError()));
                }
                
                var subscription = message.getContent().subscription;
                self._mapSubscriptionTopic[subscription] = topic;
                
                return cb(null, message);
                
            } catch (e) {
                logger.error('EventServiceClient register call error', e);
                return cb(e);
            }
        }, addInFront);
        */
        
    } catch (e) {
        return setImmediate(cb, e);
    }
};

/**
 * Unsubscribe from a topic
 * @param {type} topic
 * @param {type} cb
 * @returns {unresolved}
 */
o.unsubscribe = function (topic, cb) {
    try {
        var self = this;
        
        if (!_.isFunction(cb)) {
            logger.warn('publish: callback not provided!');
            return;
        }
        
        if (!_.isString(topic) || !topic.length) {
            return setImmediate(cb, new Error(errors.ERR_INVALID_TOPIC));
        }
        
        var subscription = this._getSubscriptionByTopic(topic);
        
        if (subscription === null) {
            return setImmediate(cb, new Error(errors.ERR_SUBSCRIPTION_ID_NOT_FOUND));
        }
        
        var message = new ProtocolMessage();
        
        message.setMessage(self._eventServiceName + '/unsubscribe');
        message.setSeq(self._getSeq());
        message.setContent({
            subscription: '' + subscription
        });
        
        self._request(message, function (err, message) {
            try {
                delete self._mapSubscriptionTopic[subscription];
                
                if (err) {
                    return cb(err);
                }
                
                return cb(null, message);
                
            } catch (e) {
                return cb(e);
            }
        });
        
    } catch (e) {
        return setImmediate(cb, e);
    }
};

o.publish = function (topic, notificationContent, _virtual, _cb) {
    try {
        var self = this;
        var virtual;
        var cb;
        if (arguments.length === 4) {
            cb = _cb;
            virtual = _virtual;
        } else {
            cb = _virtual;
            virtual = false;
        }
        
        if (!_.isFunction(cb)) {
            logger.warn('publish: callback not provided!');
            return;
        }
        
        if (!_.isString(topic) || !topic.length) {
            return setImmediate(cb, new Error(errors.ERR_INVALID_TOPIC));
        }
        
        if (_.isUndefined(notificationContent)) {
            return setImmediate(cb, new Error(errors.ERR_NOTIFICATION_CONTENT_IS_REQUIRED));
        }
        
        
        var message = new ProtocolMessage();
        
        message.setMessage(self._eventServiceName + '/publish');
        message.setSeq(self._getSeq());
        message.setContent({
            topic: topic,
            'notification-content': notificationContent,
            virtual: virtual
        });
        
        self._request(message);
        
        return setImmediate(cb, null);
        
    } catch (e) {
        return setImmediate(cb, e);
    }
   
};

/**
 * Handler for message event. Emited by default client. If messge is notifySubscriber then emit 'event:TOPIC' event.
 * @param {ProtocolMessaage} message
 * @returns {undefined}
 */
o._onMessageHandler = function (message) {
    try {
        
        var self = this;
        if (message.getMessageName() === 'notifySubscriber') {
            var content = message.getContent();
            var subscription = content.subscription;
            var topic = content.topic;
            
            if (_.isUndefined(self._mapSubscriptionTopic[subscription])) {
                logger.warn('No subscription for subscription=' + subscription + ', topic=' + topic);
                return;
            }
            
            if (self._mapSubscriptionTopic[subscription] !== topic) {
                logger.warn('Topics doesn match, existing topic=' + self._mapSubscriptionTopic[subscription] + ', received topic=' + topic);
                return;
            }
            
            
            self.emit('event:' + content.topic, content['notification-content']);
        }
    } catch (e) {
        logger.error('_onMessageHandler', e, message);
    }
};

o._getSeq = function () {
    return ++this._seq;
};


/**
 * Resubscribe to existing topics when connection to event service is estabilished again.
 * @returns {e|null}
 */
o._onReconnectedHandler = function (eventCallback) {
    try {
        logger.debug('EventServiceClient _onReconnectedHandler called');
        var self = this;
        if (self._onReconnectedHandlerRunning) {
            logger.error('EventServiceClient _onReconnectedHandler already running');
            return setImmediate(eventCallback);
        }
        self._onReconnectedHandlerRunning = true;
        
        var subscriptions = [];
        for (var subscription in self._mapSubscriptionTopic) {
            subscriptions.push({
                subscription: subscription*1,
                topic: self._mapSubscriptionTopic[subscription]
            });
        }
        
        var message = new ProtocolMessage();
        message.setMessage(self._eventServiceName + '/resubscribe');
        message.setSeq(self._getSeq());
        message.setContent({
            subscriptions: subscriptions
        });
        
        // send resubscribe message. add the message in front of the queue
        self._defaultClient.sendMessage(message, function (err, message) {
            self._onReconnectedHandlerRunning = false;
            try {
                if (err) {
                    logger.error('_onReconnectedHandler error on resubscription', err);
                    return;
                }
                
                var subscriptions = message.getContent()['subscriptions'];
                for (var i=0; i<subscriptions.length; i++) {
                    var item = subscriptions[i];
                    if (item.subscription !== item.resubscription) {
                        delete self._mapSubscriptionTopic[item.subscription];
                        self._mapSubscriptionTopic[item.resubscription] = item.topic;
                    }
                }
                
            } catch (e) {
                logger.error('_onReconnectedHandler error handling resubscription', err);
                return;
            }
        }, true);
        
        return setImmediate(eventCallback);
        
    } catch (e) {
        self._onReconnectedHandlerRunning = false;
        logger.error('_onReconnectedHandler', e);
        return setImmediate(eventCallback);
    }
};


o._onReconnectedToNewInstanceHandler = function (eventCallback) {
    try {
        logger.debug('EventServiceClient _onReconnectedToNewInstanceHandler called');
        var self = this;
        if (self._onReconnectedToNewInstanceHandlerRunning) {
            logger.error('EventServiceClient _onReconnectedToNewInstanceHandler already running');
            return setImmediate(eventCallback);
        }
        self._onReconnectedToNewInstanceHandlerRunning = true;
        
        self._subscribeToNewEventService(function (err) {
            self._onReconnectedToNewInstanceHandlerRunning = false;
            if (err) {
                logger.error('EventServiceClient error trying to resubscribe to a new instance', err);
            } else {
                logger.debug('EventServiceClient subscribe to new event service success');
            }
        });
        
        return setImmediate(eventCallback);
        
    } catch (e) {
        self._onReconnectedToNewInstanceHandlerRunning = false;
        logger.error('EventServiceClient _onReconnectedToNewInstanceHandler error', e);
        return setImmediate(eventCallback);
    }
};


/**
 * Subscribe to existing topics using the new event service.
 * @param {callback} cb
 * @returns {unresolved}
 */
o._subscribeToNewEventService = function (cb) {
    try {
        var self = this;
        var items = [];
        
        for(var k in self._mapSubscriptionTopic) {
            items.push(self._mapSubscriptionTopic[k]);
        }
        
        
        if (!items.length) {
            return setImmediate(cb, false);
        }
        
        logger.debug('Subscribe to new event sevice, items=', items);
        
        
        // reset mapping
        self._mapSubscriptionTopic = {};
        
        // subscribe to all topics. add message in front of the queue
        async.mapSeries(items, function (topic, cbSub) {
            self.subscribe(topic, cbSub, true);
        }, cb);
        
    } catch (e) {
        return setImmediate(cb, e);
    }
};


/**
 * Get subscription by topic. null if topic not exists
 * @param {e|string|null} topic
 */
o._getSubscriptionByTopic = function (topic) {
    try {
        for (var subscription in this._mapSubscriptionTopic) {
            if (this._mapSubscriptionTopic[subscription] === topic) {
                return subscription;
            }
        }
        
        return null;
    } catch (e) {
        return e;
    }
};

/**
 * Get topic by subscription. null if subscription not exists
 * @param {number} subscription
 * @returns {e|string|null}
 */
o._getTopicBySubscription = function (subscription) {
    try {
        
        if (!_.isUndefined(this._mapSubscriptionTopic[subscription])) {
            return this._mapSubscriptionTopic[subscription];
        }
        return null;
        
    } catch (e) {
        return e;
    }
};

o.disconnect = function (code, reason) {
    try {
        var _code = code || 1000;
        var _reason = reason || 'client_close';
        this._defaultClient.disconnect(_code, _reason, true);
    } catch (e) {
        logger.error('EventServiceClient.disconnect() tc error:', e);
        throw e;
    }
};


module.exports = EventServiceClient;