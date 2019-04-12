angular.
module('appDebugger').
directive('network', function(modal){
    return {
        restrict: 'EA',
        scope: false,
        templateUrl: 'javascript/app-debugger/app-debugger-network.template.html',
        link: function(scope, element) {
            scope.$ctrl.networkNodes = new vis.DataSet([]);
            scope.$ctrl.networkEdges = new vis.DataSet([]);

            // provide the data in the vis format
            var data = {
                nodes: scope.$ctrl.networkNodes,
                edges: scope.$ctrl.networkEdges
            };
            var options = {
                nodes: {
                    shape: "box"
                },
                edges: {
                    arrows: {
                        to: {
                            enabled: true
                        }
                    }
                },
                physics: {
                    solver: "forceAtlas2Based"
                }
            };

            // initialize your network!
            scope.$ctrl.network = new vis.Network(element[0].firstChild, data, options);

            var onSelect = function(properties) {
                if (properties.nodes.length === 0) {
                    return;
                }
                modal.showDialog(scope.$ctrl.networkNodes.get(properties.nodes[0]));
            };
            scope.$ctrl.network.on('select', onSelect);
        }
    }
});