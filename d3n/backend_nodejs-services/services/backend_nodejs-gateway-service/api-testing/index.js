var cp = require('child_process'),
    fs = require('fs')
;

var instances = fs.readdirSync(__dirname + '/instances/');
for(var i=0; i<instances.length; i++) {
    cp.fork(__dirname + '/instances/' + instances[i]);
}

// wait 3 seconds , must be implemented a mechanism to detect when instances are ready
setTimeout(function () {
    // start the gateway
    cp.fork(__dirname + '/gateway.js');
    
}, 3000);
