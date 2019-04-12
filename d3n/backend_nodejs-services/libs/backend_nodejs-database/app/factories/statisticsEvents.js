// Singleton statistic events class which extends the base nodejs events class
module.exports = function (){
    var EventEmitter = require('events').EventEmitter;
    var util = require('util');

    var StatisticsEvents = function () {
        if(arguments.callee._singletonInstance) {
            return arguments.callee._singletonInstance;
        }

        arguments.callee._singletonInstance = this;  

        EventEmitter.call(this);
    }

    util.inherits(StatisticsEvents, EventEmitter);

    return new StatisticsEvents();
}();