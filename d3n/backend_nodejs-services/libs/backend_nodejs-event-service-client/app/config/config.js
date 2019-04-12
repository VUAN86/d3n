var config = {
    registryServiceURIs: process.env.REGISTRY_SERVICE_URIS || 'wss://localhost:4000',
    eventServiceName: 'event'
};
module.exports = config;