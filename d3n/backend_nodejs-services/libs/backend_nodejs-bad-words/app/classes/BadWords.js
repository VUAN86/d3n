var fs = require('fs');
var _ = require('lodash');
var errors = require('../config/errors.js');

function BadWords() {
    this._badWords = {};
    
    this._badWordsLoaded = false;
};

var o = BadWords.prototype;
 


/**
 * Check if an object field values contain bad words. 
 * @param {object} entity
 * @param {array} fields
 * @param {function} cb
 * @returns {Error|Boolean}
 */
o.hasBadWords = function (entity, fields, cb) {
    try {
        
        var self = this;
        
        self._loadBadWords(function (err) {
            try {
                if (err) {
                    return cb(err);
                }
                
                // if entity do not contain specified fields return error
                for(var i=0; i<fields.length; i++) {
                }
                
                for(var i=0; i<fields.length; i++) {
                    if(!entity.hasOwnProperty(fields[i])) {
                        continue;
                    }
                    
                    var str = entity[fields[i]];
                    
                    var res = self._stringHasBadWords(str);
                    
                    if (res instanceof Error) {
                        return cb(res);
                    }
                    
                    if (res === true) {
                        return cb(false, true);
                    }
                }

                return cb(false, false);
                
            } catch (e) {
                return cb(e);
            }
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

/**
 * Check if str contain bad words.
 * @param {string} str
 * @returns {e|Boolean}
 */
o._stringHasBadWords = function (str) {
    try {
        
        for(var i=0; i<this._badWords.length; i++) {
            var regexp1 = new RegExp(' ' + this._badWords[i] + ' ', 'i');
            var regexp2 = new RegExp(' ' + this._badWords[i], 'i');
            var regexp3 = new RegExp(this._badWords[i] + ' ', 'i');
            if (regexp1.test(str) === true || regexp2.test(str) === true || regexp3.test(str) === true) {
                return true;
            }
        }
        
        return false;
    } catch (e) {
        return e;
    }
};

/**
 * Load bad words.
 * @param {function} cb
 * @returns {unresolved}
 */
o._loadBadWords = function (cb) {
    try {
        var self = this;
        if(self._badWordsLoaded === true) {
            return setImmediate(cb, false);
        }
        fs.readFile(__dirname + '/../config/bad-words', 'utf8', function (err, content) {
            try {
                if (err) {
                    return cb(err);
                }
                
                if (!_.isString(content)) {
                    return cb(new Error(errors.BadWordsNotAString));
                }
                
                self._badWords = content.trim().replace(/\r\n/g, '\n').split('\n');
                
                this._badWordsLoaded = true;
                
                return cb(false);
                
            } catch (e) {
                return cb(e);
            }
            
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

module.exports = BadWords;