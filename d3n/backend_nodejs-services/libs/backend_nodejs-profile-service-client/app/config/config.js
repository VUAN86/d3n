var config = {
    registryServiceURIs: process.env.REGISTRY_SERVICE_URIS || 'ws://localhost:9093',
    profileServiceName: 'profile'
};
module.exports = config;