// Author: Edvin Eshagh
//
// Date: 6/22/2015
//
// Purpose:  animate traffic from one node to another.
//
// Usage: call *.animateTraffic method, passing array of edges to animate
//
// Example:
//
//       var myNodes = new vis.DataSet([  
//         {id: 1, label: 'Node 1'}, {id: 2, label: 'Node 2'}, {id: 3, label: 'Node 3'} ]);
//      
//       var myEdges = new vis.DataSet([
//           {id:1, from:1, to:2}, {id:2, from:2, to:1},
//           {id:3, from:2, to:3}, {id:4, from:3, to:1}]);
//       
//       var container = document.getElementById('mynetwork');
//       
//       var data = {nodes: myNodes, edges: myEdges};
//      
//       var options = {};
//      
//       var network = new vis.Network(container, data, options);
//      
//        function animate() {
//         network.animateTraffic([
//             {edge:1},
//             {edge:2, trafficSize:2}, 
//             {edge:3, trafficSize:5, isBackward: true},
//             {edge:3, trafficSize:5, isBackward: true, fillColor: "green", color: "#000"},
//         ]);
//       }
 //
// Known issue: Does not render traffic animation "on top of" the edges in IE
//
(function() {
	var uniqueId = 0;

	/**
	 *
	 * @param {Array} edgesTrafficList
     * @param {number} speed in ms
	 * @param {function} onPreAnimationHandler
	 * @param {function} onPreAnimateFrameHandler
	 * @param {function} onPostAnimateFrameHandler
	 * @param {function} onPostAnimationHandler
     */
	vis.Network.prototype.animateTraffic = function(
				 edgesTrafficList,
                 speed,
				 onPreAnimationHandler, 
				 onPreAnimateFrameHandler, 
				 onPostAnimateFrameHandler, 
				 onPostAnimationHandler) {

		var id = ++uniqueId;
		var trafficCanvas = null;
		var trafficCanvasCtx = null;
		var trafficCanvasWidth = null;
		var trafficCanvasHeight= null;
        var thisNetwork = this;
        var reportedErrors = {};  // Helps to avoid reporting the same error in multiple setTimeout events

        if (!speed) {
            speed = 1000;
        }

		/**
		 *	Clears animation canvas
		 */
		var clearAnimationCanvas = function () {
			trafficCanvasCtx.save();
			trafficCanvasCtx.setTransform(1, 0, 0, 1, 0, 0);
			trafficCanvasCtx.clearRect(0,0, trafficCanvasWidth, trafficCanvasHeight);
			trafficCanvasCtx.restore();
		};
        /**
         *	Remove animation canvas
         */
        var removeAnimationCanvas = function () {
            if ( trafficCanvas !== undefined) {
                trafficCanvas.parentNode.removeChild(trafficCanvas);
            }
        };


        /**
         * Parsing edge traffic element, identifying edges in network graph
         * @param {{edge: (Object|string|number), trafficSize: (null|number), isBackward: (null|boolean), fillColor: (null|String),color: (null|String)}} edgeTraffic Edge Traffic Element
         * @returns {{edge: Object, trafficSize: (null|number), isBackward: (null|boolean), fillColor: (null|String),color: (null|String)}}
         */
        var parseEdgeTraffic = function (edgeTraffic) {
            var edge;
            if (edgeTraffic.edge) {
                edge =  edgeTraffic.edge.edgeType
                    ? edgeTraffic.edge
                    : thisNetwork.body.edges[edgeTraffic.edge.id]
                || thisNetwork.body.edges[edgeTraffic.edge]
                ;
            }
            else {
                edge = thisNetwork.body.edges[edgeTraffic];
            }

            return {
                edge: edge,
                trafficSize : edgeTraffic.trafficSize || 1,
                isBackward : edge && edgeTraffic.isBackward,
                fillColor: edgeTraffic.fillColor || "red",
                color: edgeTraffic.color || "#000"
            };
        };


        /**
         * Searches for drawing canvas or creates a new one if not yet existing
         * @returns {*} Canvas Element
         */
        var getNetworkTrafficCanvas = function() {
            trafficCanvas = thisNetwork.body
                .container.getElementsByClassName('networkTrafficCanvas'+id)[0];
            if ( trafficCanvas == undefined) {
                var frame = thisNetwork.canvas.frame;
                trafficCanvas = document.createElement('canvas');
                trafficCanvas.className = 'networkTrafficCanvas'+id;
                trafficCanvas.style.position = 'absolute';
                trafficCanvas.style.top = trafficCanvas.style.left = 0;
                trafficCanvas.style.zIndex = 1;
                trafficCanvas.style.pointerEvents='none';
                trafficCanvas.style.width = frame.style.width;
                trafficCanvas.style.height = frame.style.height;
                trafficCanvas.width = frame.canvas.clientWidth;
                trafficCanvas.height = frame.canvas.clientHeight;
                frame.appendChild(trafficCanvas);
            }
            return trafficCanvas;
        };

        /**
         * Creates Canvas Element for Edge Animation and stores it into the trafficCanvas Object
         * as well as adjusting sizes
         */
        var initializeCanvasForEdgeAnimation = function() {
            if (Object.prototype.toString.call( edgesTrafficList ) !== '[object Array]') {
                edgesTrafficList = [edgesTrafficList];
            }
            trafficCanvas = getNetworkTrafficCanvas();
            trafficCanvasCtx = trafficCanvas.getContext('2d');
            trafficCanvasWidth = trafficCanvasCtx.canvas.width;
            trafficCanvasHeight= trafficCanvasCtx.canvas.height;
            var s = thisNetwork.getScale();// edgeTraffic.edge.body.view.scale;
            var t = thisNetwork.body.view.translation; //edgeTraffic.edge.body.view.translation;
            trafficCanvasCtx.setTransform(1, 0, 0, 1, 0, 0);
            trafficCanvasCtx.translate(t.x, t.y);
            trafficCanvasCtx.scale(s, s);
        };

        /**
         * Runs a single animation frame and sets a timeout to call itself again until animation is done
         * @param {number} offset
         * @param {number} frameCounter
         */
		var animateFrame = function (offset, frameCounter) {
            clearAnimationCanvas();
            var maxOffset = .9;

            if (offset > maxOffset) {
                removeAnimationCanvas();
                if (onPostAnimationHandler) onPostAnimationHandler(edgesTrafficList);
                return;
            }
            for(var i in edgesTrafficList) {
                if (edgesTrafficList.hasOwnProperty(i)) {
                    var edgeTraffic = parseEdgeTraffic(edgesTrafficList[i]);

                    if (!edgeTraffic.edge) {
                        if (!reportedErrors[edgesTrafficList[i]]) {
                            console.error ("No edge path defined: " , edgesTrafficList[i]);
                            reportedErrors[edgesTrafficList[i]] = true;
                        }
                        continue;
                    }

                    if (onPreAnimateFrameHandler
                        && onPreAnimateFrameHandler(edgeTraffic,frameCounter) === false ) {
                        continue;
                    }

                    //noinspection JSAccessibilityCheck
                    var p = edgeTraffic.edge.edgeType.getPoint(
                        edgeTraffic.isBackward ? maxOffset - offset: offset);

                    trafficCanvasCtx.beginPath();
                    trafficCanvasCtx.arc(p.x, p.y, parseInt(edgeTraffic.trafficSize) || 1, 0, Math.PI*2, false);
                    trafficCanvasCtx.lineWidth=1;
                    trafficCanvasCtx.strokeStyle= edgeTraffic.color;
                    trafficCanvasCtx.fillStyle = edgeTraffic.fillColor;
                    trafficCanvasCtx.fill();
                    trafficCanvasCtx.stroke();
                    trafficCanvasCtx.closePath();

                    if (onPostAnimateFrameHandler
                        && onPostAnimateFrameHandler(edgeTraffic,frameCounter) === false) {
                        removeAnimationCanvas();
                        if (onPostAnimationHandler) onPostAnimationHandler(edgesTrafficList);
                        return;
                    }
                }
            }

            offset = ((frameCounter * 10) / speed);

            setTimeout(animateFrame.bind(this, offset, frameCounter + 1), 10);
		};
		

		initializeCanvasForEdgeAnimation();
		if (onPreAnimationHandler
		 && onPreAnimationHandler(edgesTrafficList) === false) return;
		animateFrame( 0.1 /*animationStartOffset*/, 0 /*frame*/);

	};
})();