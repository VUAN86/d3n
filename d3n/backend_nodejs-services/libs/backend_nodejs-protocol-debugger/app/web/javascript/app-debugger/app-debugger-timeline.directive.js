angular.
module('appDebugger').
directive('timeline', function(modal){
    return {
        restrict: 'EA',
        scope: false,
        templateUrl: 'javascript/app-debugger/app-debugger-timeline.template.html',
        link: function(scope, element) {
            scope.$ctrl.timelineEvents = new vis.DataSet([]);
            var options = {
                // Set global item type. Type can also be specified for items individually
                // Available types: 'box' (default), 'point', 'range', 'rangeoverflow'
                type: 'box',
                maxHeight: "500px",
                showMajorLabels: true,
                showCurrentTime : true,
                format: {
                    minorLabels: {
                        millisecond:'SSS',
                        second:     's',
                        minute:     'HH:mm:ss',
                        hour:       'HH:mm',
                        weekday:    'ddd D',
                        day:        'D',
                        month:      'MMM',
                        year:       'YYYY'
                    },
                    majorLabels: {
                        millisecond:'HH:mm:ss',
                        second:     'D MMMM HH:mm',
                        minute:     'ddd D MMMM HH',
                        hour:       'ddd D MMMM',
                        weekday:    'MMMM YYYY',
                        day:        'MMMM YYYY',
                        month:      'YYYY',
                        year:       ''
                    }
                }
            };

            scope.$ctrl.timeline = new vis.Timeline(element[0], scope.$ctrl.timelineEvents, options);
            scope.$ctrl.timeline.addCustomTime(scope.$ctrl.customTime, "simulationTime");
            scope.$ctrl.timeline.on('timechange', scope.$ctrl.timelineChangeHandler);
            scope.$ctrl.timeline.on('timechanged', scope.$ctrl.timelineChangedHandler);

            var onSelect = function(properties) {
                if (properties.items.length === 0) {
                    return;
                }
                modal.showDialog(scope.$ctrl.timelineEvents.get(properties.items[0]));
            };
            scope.$ctrl.timeline.on('select', onSelect);
        }
    }
});