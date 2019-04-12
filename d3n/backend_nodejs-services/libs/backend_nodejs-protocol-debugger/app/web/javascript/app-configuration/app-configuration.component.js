angular.
    module('appConfiguration').
    component('appConfiguration', {
        templateUrl: 'javascript/app-configuration/app-configuration.template.html',
        controller: function($http, $timeout, $location) {
            this.information = {config: {ip: "", port: 0}, files: []};
            this.selectedFiles = {};
            this.liveUpdates = false;
            this.download = false;

            var self = this;

            this.loadFileList = function() {
                self.download = true;
                $http.get("/getLogFiles")
                    .then(function(response) {
                        if(response.status === 200) {
                            self.information = response.data;
                        }
                        $timeout(function() {
                            self.download = false;
                        }, 500);
                    });
            };


            this.startDebugger = function(){
                $location.path("debugger/"+JSON.stringify({selectedFiles: self.selectedFiles, liveUpdates: self.liveUpdates, ip: self.information.config.ip, port: self.information.config.port}));
            };

            this.loadFileList();
        }
});