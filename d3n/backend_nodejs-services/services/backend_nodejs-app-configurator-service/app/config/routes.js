var _ = require('lodash');
var bodyParser = require('body-parser');
var securityMiddleware = require('./../api/securityMiddleware.js');
var HttpTenantApi = require('./../api/httpTenantApi.js');
var WsTenantApi = require('./../api/wsTenantApi.js');
var WsAchievementApi = require('./../api/wsAchievementApi.js');
var WsAdvertisementApi = require('./../api/wsAdvertisementApi.js');
var WsApplicationApi = require('./../api/wsApplicationApi.js');
var WsGameApi = require('./../api/wsGameApi.js');
var WsProfileApi = require('./../api/wsProfileApi.js');;
var WsVoucherApi = require('./../api/wsVoucherApi.js');
var WsWinningApi = require('./../api/wsWinningApi.js');
var WsPromocodeApi = require('./../api/wsPromocodeApi.js');
var wsTombolaApi = require('./../api/wsTombolaApi.js');
var wsDummyApi = require('./../api/wsDummyApi.js');
var InternalEventsHandlers = require('./../events/internalEventsHandlers.js');

module.exports = function (service) {

    // HTTP message handlers
    if (_.has(service, 'http') && !_.isUndefined(service.http) && !_.isNull(service.http)) {
        // Disable IP check functionality and test per request from integration team to be able to access the
        // test machine from different IP addresses
        service.http.use([securityMiddleware.basicAuth, bodyParser.json()]);
        // service.http.use([securityMiddleware.basicAuth, securityMiddleware.IPCheck, bodyParser.json()]);

        service.http.get('/tenantManagementConnector/tenantGet', HttpTenantApi.tenantGet);
        service.http.post('/tenantManagementConnector/tenantList', HttpTenantApi.tenantList);
        service.http.post('/tenantManagementConnector/tenantCreate', HttpTenantApi.tenantUpdate);
        service.http.post('/tenantManagementConnector/tenantUpdate', HttpTenantApi.tenantUpdate);
        service.http.post('/tenantManagementConnector/tenantBulkUpdate', HttpTenantApi.tenantBulkUpdate);
        service.http.post('/tenantManagementConnector/administratorDelete', HttpTenantApi.administratorDelete);

        service.http.get('/tenantManagementConnector/tenant/:tenantId/contractGet', HttpTenantApi.contractGet);
        service.http.post('/tenantManagementConnector/tenant/:tenantId/contractList', HttpTenantApi.contractList);
        service.http.post('/tenantManagementConnector/tenant/:tenantId/contractCreate', HttpTenantApi.contractUpdate);
        service.http.post('/tenantManagementConnector/tenant/:tenantId/contractUpdate', HttpTenantApi.contractUpdate);
        service.http.post('/tenantManagementConnector/tenantConfigurationUpdate', HttpTenantApi.tenantConfigurationUpdate);
        service.http.post('/tenantManagementConnector/tenantPaydentApiKeyUpdate', HttpTenantApi.tenantPaydentApiKeyUpdate);
        service.http.post('/tenantManagementConnector/tenantExchangeCurrencyUpdate', HttpTenantApi.tenantExchangeCurrencyUpdate);

        service.http.get('/tenantManagementConnector/tenant/:tenantId/administratorGet', HttpTenantApi.administratorGet);
        service.http.get('/tenantManagementConnector/tenant/:tenantId/administratorImpersonate', HttpTenantApi.administratorImpersonate);
        service.http.post('/tenantManagementConnector/tenant/:tenantId/administratorCreate', HttpTenantApi.administratorCreate);
        service.http.post('/tenantManagementConnector/f4mConfigurationUpdate', HttpTenantApi.f4mConfigurationUpdate);

        // Audit logs
        service.http.post('/tenantManagementConnector/tenantTrailList', HttpTenantApi.tenantAuditLogList);
        service.http.post('/tenantManagementConnector/contractTrailList', HttpTenantApi.contractAuditLogList);
        service.http.post('/tenantManagementConnector/administratorTrailList', HttpTenantApi.administratorAuditLogList);
        
        // Tenant invoice 
        service.http.post('/tenantManagementConnector/tenant/:tenantId/invoiceCreate', HttpTenantApi.invoiceCreate);
        
        // Tenant invoice for end customer
        service.http.post('/tenantManagementConnector/tenant/:tenantId/invoiceEndCustomerCreate', HttpTenantApi.invoiceEndCustomerCreate);

        // VAT
        service.http.post('/tenantManagementConnector/vatAdd', HttpTenantApi.vatAdd);
        
        // profileGet
        service.http.post('/tenantManagementConnector/profileGet', HttpTenantApi.profileGet);
        
        //tenantAdminContractCreate
        service.http.post('/tenantManagementConnector/tenantAdminContractCreate', HttpTenantApi.tenantAdminContractCreate);
    }

    // WS message handlers
    if (_.has(service, 'ws') && !_.isUndefined(service.ws) && !_.isNull(service.ws)) {

        _.each(_.functions(WsTenantApi), function (name) {
            service.ws.messageHandlers[name] = WsTenantApi[name];
        });
        _.each(_.functions(WsProfileApi), function (name) {
            service.ws.messageHandlers[name] = WsProfileApi[name];
        });
        _.each(_.functions(WsAchievementApi), function (name) {
            service.ws.messageHandlers[name] = WsAchievementApi[name];
        });
        _.each(_.functions(WsAdvertisementApi), function (name) {
            service.ws.messageHandlers[name] = WsAdvertisementApi[name];
        });
        _.each(_.functions(WsApplicationApi), function (name) {
            service.ws.messageHandlers[name] = WsApplicationApi[name];
        });
        _.each(_.functions(WsGameApi), function (name) {
            service.ws.messageHandlers[name] = WsGameApi[name];
        });
        _.each(_.functions(WsVoucherApi), function (name) {
            service.ws.messageHandlers[name] = WsVoucherApi[name];
        });
        _.each(_.functions(WsWinningApi), function (name) {
            service.ws.messageHandlers[name] = WsWinningApi[name];
        });
        _.each(_.functions(WsPromocodeApi), function (name) {
            service.ws.messageHandlers[name] = WsPromocodeApi[name];
        });
        _.each(_.functions(wsTombolaApi), function (name) {
            service.ws.messageHandlers[name] = wsTombolaApi[name];
        });
        _.each(_.functions(wsDummyApi), function (name) {
            service.ws.messageHandlers[name] = wsDummyApi[name];
        });
        
        // Events handlers
        _.each(_.functions(InternalEventsHandlers), function (name) {
            service.ws.eventHandlers[name] = InternalEventsHandlers[name];
        });
        
    }
};
