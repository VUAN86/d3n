angular.
module('appDebugger').
factory('websocketFeeder', function(){
    var websocketFeederInstance = {
        eventFeeder: null,
        ws: null,
        init: function(eventFeeder) {
            this.eventFeeder = eventFeeder;
        },
        connect: function(ip, port) {
            this.close();
            try {
                this.ws = new WebSocket("ws://"+ip+":"+port);
                this.ws.onmessage = function (event) {
                    try {
                        var element = JSON.parse(event.data);
                        websocketFeederInstance.eventFeeder.push(element);
                    } catch (e) {
                        console.error(event.data);
                    }
                }
            } catch (e) {
                console.error(e);
            }
        },
        close: function() {
            if (this.ws) {
                this.ws.close();
                this.ws = null;
            }
        }
    };

    return websocketFeederInstance;
});