/**
 * @type SessionDataStorage
 *
 * Used for storing various session informations
 *
 * @constructor
 */
function SessionDataStorage() {
    this.storage = {};
}
var o = SessionDataStorage.prototype;


/**
 * Set key->value
 * @param {string} key
 * @param {*} value
 */
o.set = function (key, value) {
    this.storage[key] = value;
};

/**
 * Get key value
 * @param {string} key
 * @returns {*|null}
 */
o.get = function (key) {
    if (key in this.storage) {
        return this.storage[key];
    } else {
        return null;
    }
};

module.exports = SessionDataStorage;