angular.
module('appDebugger').
component('appDebugger', {
    templateUrl: 'javascript/app-debugger/app-debugger.template.html',
    controller: function($scope, $routeParams, $interval, websocketFeeder, fileFeeder, eventFeeder) {
        var self = this;

        try {
            this.config = JSON.parse($routeParams.config);
        } catch (e) {
            this.config = { selectedFiles: {}, liveUpdates: true, ip: "127.0.0.1", port: 9090 }
        }

        this.intervalTimer = 0;
        this.intervalTimerRunning = false;
        this.customTime = new Date().getTime();
        this.customTimeString = new Date().toISOString();
        this.stepTime = 10;
        this.liveUpdates = this.config.liveUpdates;

        eventFeeder.init(this);
        fileFeeder.init(eventFeeder);
        websocketFeeder.init(eventFeeder);

        this.toggleLiveUpdates = function() {
            if (self.liveUpdates) {
                websocketFeeder.connect(this.config.ip, this.config.port);
            } else {
                websocketFeeder.close();
            }

        };

        this.changeTime = function(offset) {
            var value;
            if (offset > 60*60*24*30*12*50) {
                value = new Date(offset);
            } else {
                value = new Date(parseInt(this.customTime) + offset);
            }
            self.timeline.setCustomTime(value, "simulationTime");
            self.timelineChangeHandler({id: "simulationTime", time: value, noApply: true});
            self.timelineChangedHandler({id: "simulationTime", time: value, noApply: true});
        };

        var slowCounter = 0.0;
        var timelineIntervalFunction = function() {
            var step = self.stepTime / 10;
            if (self.stepTime < 10) {
                slowCounter += step;
                step = Math.floor(slowCounter);
            }
            self.changeTime(step);
            if (slowCounter > 1.0) {
                slowCounter -= 1.0;
            }
        };

        this.startTime = function() {
            self.timelineInterval = $interval(timelineIntervalFunction, 100);
            self.intervalTimerRunning = true;
        };

        this.stopTime = function() {
            $interval.cancel(self.timelineInterval);
            self.intervalTimerRunning = false;
        };

        this.timelineChangeHandler = function(properties) {
            if (properties.id === "simulationTime") {
                self.customTime = properties.time.getTime();
                self.customTimeString = properties.time.toISOString();
                if (!properties.noApply) {
                    $scope.$apply();
                }
            }
        };
        this.timelineChangedHandler = function(properties) {
            if (properties.id === "simulationTime") {
                self.timeline.moveTo(properties.time, {duration: 10, easingFunction: "linear"});
                eventFeeder.onTimeUpdate(properties.time);
                if (!properties.noApply) {
                    $scope.$apply();
                }
            }
        };

        this.toggleLiveUpdates();
        fileFeeder.readFiles(this.config.selectedFiles, function() {
            var length = eventFeeder.eventList.length;
            if (length > 0) {
                self.changeTime(eventFeeder.eventList[0].start.getTime() - 10);
            }

            self.timeline.fit();
        });

    }
});