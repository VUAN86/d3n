angular.
module('appDebugger').
factory('eventFeeder', function(){
    var uniqueId = 0;
    var lastUpdatedFrame = 0;
    var identifierToService = {};
    var adressToIdentifier = {};

    var eventFeederInstance = {
        controller: null,
        eventList: [],
        queue: [],
        /**
         * Initializing
         * @param controller
         */
        init: function(controller) {
            uniqueId = 0;
            lastUpdatedFrame = 0;
            identifierToService = {};
            adressToIdentifier = {};
            this.eventList = [];
            this.queue = [];
            this.controller = controller;
        }
    };

    var extractIdentifierName = function(identifier) {
        var name = identifier;
        try {
            name = identifier.split(":");
            name = name[name.length -2];
            name = name.split("/");
            name = name[name.length -1];
            name = name.split("\\");
            name = name[name.length -1];
        } catch (e) {
            //On error use the identifier as default case
        }
        return name;
    };

    var addService = function(identifier, address, name) {
        if (!identifierToService[identifier]) {
            identifierToService[identifier] = {
                addresses: { }
            }
        }

        identifierToService[identifier].addresses.address = address;
        adressToIdentifier[address] = identifier;

        if (name) {
            identifierToService[identifier].name = name;
        }
        if (!identifierToService[identifier].name) {
            identifierToService[identifier].name = extractIdentifierName(identifier);
        }
    };

    var getServiceName = function(identifierOrAddress) {
        var service;
        if (identifierToService[identifierOrAddress]) {
            service = identifierToService[identifierOrAddress];
        } else if (adressToIdentifier[identifierOrAddress] && identifierToService[adressToIdentifier[identifierOrAddress]]) {
            service = identifierToService[adressToIdentifier[identifierOrAddress]];
        } else {
            return extractIdentifierName(identifierOrAddress);
        }
        return service.name;
    };

    var getEdgesOfNode = function (nodeId) {
        var edges = eventFeederInstance.controller.networkEdges;
        return edges.get().filter(function (edge) {
            return edge.from === nodeId || edge.to === nodeId;
        });
    };
    var removeEdgesOfNode = function (nodeId) {
        var edges = eventFeederInstance.controller.networkEdges;
        var edgesOfNode = getEdgesOfNode(nodeId);
        for(var i = 0;i < edgesOfNode.length;i++) {
            edges.remove(edgesOfNode[i]);
        }
    };

    var onServiceCreated = function(event) {
        var nodes = eventFeederInstance.controller.networkNodes;
        var edges = eventFeederInstance.controller.networkEdges;
        //First: Service needs to exist
        if (!nodes.get(event.data_identification)) {
            nodes.add({id: event.data_identification, label: getServiceName(event.data_identification), data: event, color: '#228B22'});
        }
        //Second: Create socket
        if (!nodes.get(event.data_on)) {
            nodes.add({id: event.data_on, label: event.data_on});
        }
        //Third: Create edge between socket and service
        if (!edges.get(event.data_on+"-"+event.data_identification)) {
            edges.add({id: event.data_on+"-"+event.data_identification, to: event.data_identification, from: event.data_on, color: '#cccccc', width: 5, arrows: {to: {enabled: false}}});
        }

    };
    var onServiceRemoved = function(event) {
        var nodes = eventFeederInstance.controller.networkNodes;
        //Remove all connections of socket
        removeEdgesOfNode(event.data_on);
        //Remove socket
        if (nodes.get(event.data_on)) {
            nodes.remove({id: event.data_on});
        }
        //Remove service if no existing connections anymore
        if (getEdgesOfNode(event.data_identification).length === 0 &&
            nodes.get(event.data_identification)) {
            nodes.remove({id: event.data_identification});
        }
    };

    /**
     * Creates a correspondending client/service instance for the given connectoin
     */
    var createServiceClientInstances = function(event) {
        var nodes = eventFeederInstance.controller.networkNodes;
        var edges = eventFeederInstance.controller.networkEdges;

        if (event.data_is_client) {
            //Do we have a client node?
            if (!nodes.get(event.data_identification)) {
                nodes.add({id: event.data_identification, label: getServiceName(event.data_identification), data: event, color: '#228B22'});
            }
            //Third: Create edge between socket and client
            if (!edges.get(event.data_from+"-"+event.data_identification)) {
                edges.add({id: event.data_from+"-"+event.data_identification, to: event.data_identification, from: event.data_from, color: '#cccccc', width: 5, arrows: {to: {enabled: false}}});
            }
        } else {
            //Do we have a service node?
            if (!nodes.get(event.data_identification)) {
                nodes.add({id: event.data_identification, label: getServiceName(event.data_identification), data: event, color: '#228B22'});
            }
            //Third: Create edge between socket and client
            if (!edges.get(event.data_to+"-"+event.data_identification)) {
                edges.add({id: event.data_to+"-"+event.data_identification, to: event.data_identification, from: event.data_to, color: '#cccccc', width: 5, arrows: {to: {enabled: false}}});
            }
        }
    };

    var onConnectionCreated = function(event) {
        var nodes = eventFeederInstance.controller.networkNodes;
        var edges = eventFeederInstance.controller.networkEdges;

        //Does the from item exist?
        if (!nodes.get(event.data_from)) {
            nodes.add({id: event.data_from, label: event.data_from});
        }
        //Does the to item exist?
        if (!nodes.get(event.data_to)) {
            nodes.add({id: event.data_to, label: event.data_to});
        }
        //Third: Create edge to represent connection
        if (!edges.get(event.data_from+"-"+event.data_to)) {
            edges.add({id: event.data_from+"-"+event.data_to, to: event.data_to, from: event.data_from});
        }

        createServiceClientInstances(event);
    };

    var onConnectionRemoved = function(event) {
        var nodes = eventFeederInstance.controller.networkNodes;

        //Remove all connections of client socket
        removeEdgesOfNode(event.data_from);
        //Remove client socket
        if (nodes.get(event.data_from)) {
            nodes.remove({id: event.data_from});
        }
        //PS: Server socket can exist without connections, but should be removed if he has no service attached
        if (getEdgesOfNode(event.data_to).length === 0 &&
            nodes.get(event.data_to)) {
            nodes.remove({id: event.data_to});
        }

        //Do we have a client node - remove him!?
        if (event.data_is_client &&
            getEdgesOfNode(event.data_identification).length === 0 &&
            nodes.get(event.data_identification)) {
            nodes.remove({id: event.data_identification});
        }
    };

    var onUnknownMessageReceived = function(event) {
        var nodes = eventFeederInstance.controller.networkNodes;
        var edges = eventFeederInstance.controller.networkEdges;

        //Does the from item exist?
        if (!nodes.get(event.data_from)) {
            nodes.add({id: event.data_from, label: event.data_from, color: '#FF8A8A'});
        }
        //Does the to item exist?
        if (!nodes.get(event.data_to)) {
            nodes.add({id: event.data_to, label: event.data_to, color: '#FF8A8A'});
        }
        //Third: Create edge to represent connection
        if (!edges.get(event.data_from+"-"+event.data_to)) {
            edges.add({id: event.data_from+"-"+event.data_to, to: event.data_to, from: event.data_from, color: '#FF8A8A'});
        }

        createServiceClientInstances(event);
    };

    var onDataFlow = function(event, backwards) {
        var edges = eventFeederInstance.controller.networkEdges;
        var network = eventFeederInstance.controller.network;

        var idForward = event.data_from+"-"+event.data_to;
        var idBackward = event.data_to+"-"+event.data_from;
        var idToUse = idForward;
        var backward = false;
        if (!edges.get(idToUse)) {
            backward = true;
            idToUse = idBackward;
        }
        if (!edges.get(idToUse)) {
            idToUse = idForward;
            onUnknownMessageReceived(event);
        }
        var trafficSize = Math.floor( event.content.length / 2 );

        if (backwards) {
            backward = !backward;
        }

        var color = '#D5DDF6';
        if (event.data_action === "received") {
            color = '#4F94CD';
        }

        var sequentialEdgesToAnimate= [
            {edge:idToUse, trafficSize:trafficSize, isBackward: backward, fillColor: color}
        ];
        network.animateTraffic(
            // first edge to start animating
            sequentialEdgesToAnimate [0] ,
            // speed
            500,
            // onPreAnimationHandler
            null,
            //onPreAnimateFrameHandler
            null ,
            //onPostAnimateFrameHandler
            null ,
            // onPostAnimationHandler
            function() {
                //Nothing to do
            }
        );
    };

    /**
     * Applies event changes in network graph
     * @param event
     * @param backwards
     */
    var applyChange = function(event, backwards) {
        if (event.data_type === "service") {
            if (event.data_action === "created" && !backwards || event.data_action === "removed" && backwards) {
                onServiceCreated(event);
            } else {
                onServiceRemoved(event);
            }
        } else if (event.data_type === "connection") {
            if (event.data_action === "created" && !backwards || event.data_action === "removed" && backwards) {
                onConnectionCreated(event);
            } else {
                onConnectionRemoved(event);
            }
        } else if (event.data_type === "data" && Math.abs(lastUpdatedFrame.getTime() - event.start.getTime()) < 100 ) {
            onDataFlow(event, backwards);
        }
    };

    /**
     * Searches for changes in the timeline and applies it to the network graph
     * @param time
     */
    var updateNetwork = function(time) {
        var events = eventFeederInstance.eventList;

        var changes = [];
        if (events.length === 0) {
            return;
        }

        //First gather all differences between lastUpdatedTimeFrame and now:
        var minIndex = 0;
        var maxIndex = events.length - 1;
        var index = 0;
        var backwards = false;
        if (time > lastUpdatedFrame) {
            while(index <= maxIndex && events[index].start < lastUpdatedFrame) {
                index++;
            }
            while(index <= maxIndex && events[index].start < time) {
                changes.push(events[index]);
                index++;
            }


        } else if (time < lastUpdatedFrame) {
            index = maxIndex;
            while(index >= minIndex && events[index].start > lastUpdatedFrame) {
                index--;
            }
            while(index >= minIndex && events[index].start > time) {
                changes.push(events[index]);
                index--;
            }
            backwards = true;
        }

        lastUpdatedFrame = time;

        for (var i = 0;i < changes.length; i++) {
            applyChange(changes[i], backwards);
        }
    };

    /**
     * Adds service event timeline
     * @param element
     * @returns {*}
     */
    var onAddEventService = function(element) {
        var on = element.data_on;
        element.data_on = on.replace("localhost", "127.0.0.1");
        addService(event.identification, element.data_on, element.data_visibleName);
        element.content = element.data_visibleName+" service "+event.action+"<br>"+element.data_on;
        if (event.action === "created") {
            element.style="background-color: #228B22;";
        } else {
            element.style="background-color: #FF2400;";
        }
        return element;

    };

    /**
     * Adds connection event timeline
     * @param element
     * @returns {*}
     */
    var onAddEventConnection = function(element) {
        if (element.data_is_client) {
            addService(element.data_identification, element.data_from);
        } else {
            addService(element.data_identification, element.data_to);
        }
        if (element.data_action === "removed") {
            element.style="background-color: #FF7F24;";
        } else if (element.data_action === "created") {
            element.style="background-color: #CAFF70;";
        }
        element.data_fromName = getServiceName(element.data_from);
        element.data_toName = getServiceName(element.data_to);
        element.content = "connection "+element.data_action+"<br>"+element.data_from+" -> "+element.data_to;
        return element;
    };

    /**
     * Adds data event timeline
     * @param element
     * @returns {*}
     */
    var onAddEventData = function(element) {
        if (element.data_action === "received") {
            addService(element.data_identification, element.data_to);
            element.style="background-color: #4F94CD;";
        } else {
            addService(element.data_identification, element.data_from);
        }
        element.content = element.data_action+" "+element.data_message.message;
        return element;
    };

    var addEvent = function(event) {
        var element = {
            id: ++uniqueId,
            start: event.timestamp
        };

        for (var key in event) {
            if (event.hasOwnProperty(key)) {
                element['data_'+key] = event[key];
            }
        }

        if (event.type === "service") {
            element = onAddEventService(element);
        } else
        if (event.type === "connection") {
            element = onAddEventConnection(element);
        } else
        if (event.type === "data") {
            element = onAddEventData(element);
        }

        eventFeederInstance.queue.push(element);

    };

    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================

    var merge = function(left, right){
        var result = [],
            lLen = left.length,
            rLen = right.length,
            l = 0,
            r = 0;
        while(l < lLen && r < rLen){
            if(left[l].start < right[r].start){
                result.push(left[l++]);
            }
            else{
                result.push(right[r++]);
            }
        }
        //remaining part needs to be addred to the result
        return result.
        concat(left.slice(l)).
        concat(right.slice(r));
    };
    var mergeSort = function(arr){
        var len = arr.length;
        if(len <2)
            return arr;
        var mid = Math.floor(len/2),
            left = arr.slice(0,mid),
            right =arr.slice(mid);
        //send left and right to the mergeSort to broke it down into pieces
        //then merge those
        return merge(mergeSort(left), mergeSort(right));
    };

    /**
     * We have an almost sorted array (maybe some timestamps are little bit off,
     * but most of the time we have a fully sorted array already ->
     * Bubble Sort is the best algorithm
     * @param a
     */
    var bubbleSort = function (a)
    {
        var swapped;
        do {
            swapped = false;
            for (var i= a.length-1; i > 0; i--) {
                if (a[i].start > a[i-1].start) {
                    var temp = a[i];
                    a[i] = a[i-1];
                    a[i-1] = temp;
                    swapped = true;
                }
            }
        } while (swapped);
    };


    /**
     * Pushes an event
     * @param event
     * @param {boolean} queue True if it should not be flushed immediatly
     */
    eventFeederInstance.push = function(event, queue) {
            try {
                if (event.message && event.message.utf8Data) {
                    event.message = JSON.parse(event.message.utf8Data);
                } else if (event.message && (typeof event.message === 'string' || event.message instanceof String)) {
                    event.message = JSON.parse(event.message);
                } else if (event.message) {
                    event = event.message;
                }
            } catch (e) {
                console.error(e);
                console.error(event);
            }
            event.timestamp = new Date(event.timestamp);

            addEvent(event);

            if (!queue) {
                eventFeederInstance.flush();
            }
    };

    /**
     * Flushes all received events into the timeline
     */
    eventFeederInstance.flush = function() {
            var length = eventFeederInstance.queue.length;
            for (var i = 0;i < length;i++) {
                eventFeederInstance.eventList.push(eventFeederInstance.queue[i]);
                eventFeederInstance.controller.timelineEvents.add(eventFeederInstance.queue[i]);
            }
            eventFeederInstance.queue = [];
            if (length < 3) {
                //low memory consumption, slow on bigger entries
                bubbleSort(eventFeederInstance.eventList);
            } else {
                //higher memory usage, faster on more changes
                eventFeederInstance.eventList = mergeSort(eventFeederInstance.eventList);
            }
        };

    /**
     * Updates network graph
     * @param time
     */
    eventFeederInstance.onTimeUpdate = function(time) {
        updateNetwork(time);
    };

    return eventFeederInstance;
});