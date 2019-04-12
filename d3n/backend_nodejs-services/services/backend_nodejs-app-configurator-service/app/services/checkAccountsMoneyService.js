const Config = require('./../config/config.js');
const Errors = require('./../config/errors.js');
const DefaultClient = require('nodejs-default-client');
const ProtocolMessage = require('nodejs-protocol');
const RegistryServiceClient = require('nodejs-service-registry-client');
const Database = require('nodejs-database').getInstance(Config);
const Application = Database.RdbmsService.Models.Application.Application;


const paymentServiceName = 'payment';
const paymentServiceClient = new DefaultClient({
  serviceNamespace: paymentServiceName,
  reconnectForever: false,
  autoConnect: true,
  serviceRegistryClient: new RegistryServiceClient({
    registryServiceURIs: Config.registryServiceURIs
  }),
  headers: { service_name: 'checkAccountsMoneyService' }
});

const getApp = (appId) => {
  return Application.findOne({ where: { id: appId } })
    .then((application) => {
      if (!application) {
        throw Errors.DatabaseApi.NoRecordFound;
      }
      return application;
    });
};

const getAccountBalance = (tenantId, currency) => {
  return new Promise((resolve, reject) => {
    const pm = new ProtocolMessage();
    pm.setMessage(paymentServiceName + '/getAccountBalance');
    pm.setSeq(paymentServiceClient.getSeq());
    pm.setContent({
      tenantId: tenantId + '',
      currency
    });
    paymentServiceClient.sendMessage(pm, (err, response) => {
      if (err) {
        return reject(err);
      }

      if (response.getError()) {
        return reject(new Error(response.getError().message));
      }

      return resolve(response.getContent().amount);
    });
  });
};

const checkAccountBalance = (tenantId, currency, cost) => {
  return getAccountBalance(tenantId, currency)
    .then((balance) => {
      return balance >= cost;
    });
};

const checkAppBalance = (appId, currency, cost) => {
  return getApp(appId)
    .then((app) => {
      return checkAccountBalance(
        app.tenantId,
        currency,
        cost
      );
    });
};

const checkAppsBalance = (appIds, currency, cost) => {
  let result = true;

  return Promise.all(appIds.map((id) =>
    checkAppBalance(id, currency, cost)
      .then((res) => {
        result = result && res;
      })))
    .then(() => result);
};


module.exports = {
  checkGameBalance: (game, callback) => checkAppsBalance(
    game.applications.map((res) => res.appId),
    game.entryFeeCurrency,
    game.resultConfiguration.minimumJackpotAmount
  )
    .then((result) => {
      if (callback) {
        return callback(null, result);
      }
      return null;
    })
    .catch((err) => {
      if (callback) {
        return callback(err);
      }
      throw err;
    })
};
