var FakeServiceRegistry = require('./classes/FakeRegistryService.js'),
    workerConfig = JSON.parse(process.argv[2]),
    serviceConfig = workerConfig.service
;

var fakeServiceRegistry = new FakeServiceRegistry(serviceConfig);

fakeServiceRegistry.build(function (err) {
    process.send({
        success: err ? false : true
    });
});