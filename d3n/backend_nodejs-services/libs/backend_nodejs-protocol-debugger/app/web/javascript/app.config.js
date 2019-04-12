angular.
module('protocolDebuggerApp').
config(['$locationProvider', '$routeProvider',
    function($locationProvider, $routeProvider) {
        $locationProvider.hashPrefix('!');

        $routeProvider.
        when('/configuration', {
            template: '<app-configuration></app-configuration>'
        }).
        when('/debugger/:config', {
            template: '<app-debugger></app-debugger>'
        }).
        when('/debugger', {
            template: '<app-debugger></app-debugger>'
        }).
        otherwise('/configuration');
    }
]);