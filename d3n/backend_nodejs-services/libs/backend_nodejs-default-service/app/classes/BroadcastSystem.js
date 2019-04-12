var _ = require('lodash');

/**
 * @typedef {object} BroadcastSystem
 *
 * The BroadcastSystem enables services to register clients
 * in rooms and broadcast messages to all participants
 * @constructor
 */
function BroadcastSystem() {
    this._rooms = {};
}

var o = BroadcastSystem.prototype;

/**
 * Add a client seesion in a room. Create room if not exists.
 * @param {string} room
 * @param {ClientSession} clientSession
 * @returns {boolean} true on success, false if not successful
 */
o.add = function (room, clientSession) {
    if ( !_.isArray(this._rooms[room]) ) {
        this._rooms[room] = [];
    }
    if ( this.isInRoom(room, clientSession) ) {
        // if already in room, do nothing
        return false;
    }
    this._rooms[room].push(clientSession);
    return true;
};


/**
 * Remove specified client session from specified room.
 * @param {string} room
 * @param {ClientSession} clientSession
 */
o.remove = function (room, clientSession) {
    if (!this._rooms[room]) {
        return;
    }

    var idx = this._rooms[room].indexOf(clientSession);
    if (idx >= 0) {
        this._rooms[room].splice(idx, 1);
    }
};

/**
 * Remove the client seesion from all rooms.
 * @param {ClientSession} clientSession
 */
o.removeFromAllRooms = function (clientSession) {
    for(var room in this._rooms) {
        if (this._rooms.hasOwnProperty(room)) {
            this.remove(room, clientSession);
        }
    }
};

/**
 * Return true if specified client session is in the specified room, false otherwise.
 * @param {string} room
 * @param {ClientSession} clientSession
 * @returns {Boolean}
 */
o.isInRoom = function (room, clientSession) {
    if (!this._rooms[room]) {
        return false;
    }

    return this._rooms[room].indexOf(clientSession) >= 0;
};

/**
 * Send message to all clients that belongs to specified rooms.
 * @param {string} room
 * @param {ProtocolMessage} message
 */
o.sendMessage = function (room, message) {
    if (!this._rooms[room]) {
        return false;
    }

    var sessions = this._rooms[room];
    for(var i=0; i<sessions.length; i++) {
        message.setClientId(sessions[i].getId());
        sessions[i].sendMessage(message);
    }
};


/**
 * Get all users that belongs to provided room.
 * @param {string} room
 * @returns {ClientSession[]}
 */
o.getUsersByRoom = function (room) {
    if (this._rooms[room]) {
        return this._rooms[room];
    } else {
        return [];
    }
};

module.exports = BroadcastSystem;