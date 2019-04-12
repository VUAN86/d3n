angular.
module('appDebugger').
factory('fileFeeder', function($http){
    var fileFeederInstance = {
        eventFeeder: null,
        init: function(eventFeeder) {
            this.eventFeeder = eventFeeder;
        },
        _transformResponse: function(value) {
            var values = value.split("\n");
            var result = [];
            for (var i = 0; i < values.length;i++) {
                var element = null;
                try {
                    element = JSON.parse(values[i]);
                    result.push(element);
                } catch(e) {
                    console.error(e);
                    console.error(element);
                }
            }
            return result;
        },
        _countFiles: function(fileList) {
            var size = 0;
            for (var file in fileList) {
                if (fileList.hasOwnProperty(file)) {
                    size++;
                }
            }
            return size;
        },
        /**
         * Reading all files in fileList, calling callback on finish
         * @param fileList
         * @param cb
         */
        readFiles: function(fileList, cb) {
            var collected = 0;
            var size = this._countFiles(fileList);


            var fileResponse = function(response) {
                collected++;
                if(response.status === 200) {
                    for (var i = 0; i < response.data.length;i++) {
                        fileFeederInstance.eventFeeder.push(response.data[i], true);
                    }
                    fileFeederInstance.eventFeeder.flush();
                }
                if (collected >= size) {
                    cb();
                }
            };

            for (var file in fileList) {
                if (fileList.hasOwnProperty(file)) {
                    $http({
                        url: "/logs/"+file,
                        method: 'GET',
                        transformResponse: this._transformResponse
                    })
                    .then(fileResponse);
                }
            }

            if (size === 0) {
                setTimeout(cb, 100);
            }
        }

    };

    return fileFeederInstance;
});