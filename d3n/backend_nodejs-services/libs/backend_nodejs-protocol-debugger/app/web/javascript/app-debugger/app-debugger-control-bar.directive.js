angular.
module('appDebugger').
directive('controlBar', function(){
    return {
        restrict: 'EA',
        scope: false,
        templateUrl: 'javascript/app-debugger/app-debugger-control-bar.template.html'
    }
});