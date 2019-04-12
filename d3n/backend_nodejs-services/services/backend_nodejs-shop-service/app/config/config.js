var _ = require('lodash');
var fs = require('fs');
var path = require('path');

var Config = {
    serviceName: 'shop',
    serviceNamespaces: [
        'shop'
    ],
    numCPUs: process.env.DEBUG ? 1 : require('os').cpus().length,
    ip: process.env.HOST || 'localhost',
    port: process.env.PORT || '9096',
    secure: true,
    key: fs.readFileSync(path.join(__dirname,  'ssl-certificate/key.pem'), 'utf8'),
    cert: fs.readFileSync(path.join(__dirname,  'ssl-certificate/cert.pem'), 'utf8'),
    registryServiceURIs: process.env.REGISTRY_SERVICE_URIS || 'ws://localhost:9093',
    validateFullMessage: _.isUndefined(process.env.VALIDATE_FULL_MESSAGE) ? true : (process.env.VALIDATE_FULL_MESSAGE === 'true'),
    validatePermissions: _.isUndefined(process.env.VALIDATE_PERMISSIONS) ? false : (process.env.VALIDATE_PERMISSIONS === 'true'),
    protocolLogging: _.isUndefined(process.env.PROTOCOL_LOGGING) ? false : (process.env.PROTOCOL_LOGGING === 'true'),
    gateway: {
        ip: process.env.HOST || 'localhost',
        port: (parseInt(process.env.PORT) + 9) || '9099',
        secure: true,
        key: path.join(__dirname,  'ssl-certificate/key.pem'),
        cert: path.join(__dirname,  'ssl-certificate/cert.pem'),
        auth: {
            algorithm: 'RS256',
            publicKey: fs.readFileSync(path.join(__dirname,  'jwt-keys/pubkey.pem'), 'utf8'),
        },
    },
    auth: {
        algorithm: 'RS256',
        publicKey: fs.readFileSync(path.join(__dirname,  'jwt-keys/pubkey.pem'), 'utf8'),
        privateKey: fs.readFileSync(path.join(__dirname,  'jwt-keys/privkey.pem'), 'utf8')
    },
    jwt: {
        issuer: 'F4M Development and Testing',
        expiresInSeconds: 60 * 60 * 24 * 30 * 12 * 10, // 10 years = 10y * 12m * 30d * 24h * 60m * 60s
        impersonateForSeconds: 3600 // 1 hour 
    },
    shop: {
        url: process.env.SHOP_URL || "https://shopapp-nightly.dev4m.com/oxrest/",
        admin: {
            username: process.env.SHOP_ADMIN_USERNAME || "c.kleber@f4m.tv",
            password: process.env.SHOP_ADMIN_PASSWORD || "testtest"
        },
        authorizationPrefix: "Ox ",
        entryPoints: {
            categories: {
                url: "list/oxcategorylist",
                method: "GET"
            },
            articles: {
                url: "oxlist/oxarticlelist",
                method: "GET"
            },
            article: {
                url: "oxobject/oxarticle",
                method: "GET"
            },
            articleAdd: {
                url: "oxobject/oxarticle/1",
                method: "POST"
            },
            articleUpdate: {
                url: "oxobject/oxarticle",
                method: "PUT"
            },
            articleDelete: {
                url: "oxobject/oxarticle",
                method: "DELETE"
            },
            addBasket: {
                url: "oxbasket",
                method: "POST"
            },
            viewBasket: {
                url: "oxbasket",
                method: "GET"
            },
            orderComplete: {
                url: "oxorder/finalizeOrder",
                method: "POST"
            },
            user: {
                url: "action/createUser",
                method: "POST"
            },
            login: {
                url: "action/login",
                method: "GET"
            },
            logout: {
                url: "action/logout",
                method: "POST"
            },
            tenantAdd: {
                url: "oxobject/oxvendor/1",
                method: "POST"
            },
            tenantUpdate: {
                url: "oxobject/oxvendor",
                method: "PUT"
            },
            tenantDelete: {
                url: "oxobject/oxvendor",
                method: "DELETE"
            },
            orders: {
                url: "list/oxorder",
                method: "GET"
            },
            orderFinalize: {
                url: "oxobject/oxorder",
                method: "PUT"
            },
        },
        messages: {
            notFound: "Not found",
            alreadyExists: "Object already exists!",
            alreadyFinalized: "Order already finalized",
            deleteOk: "OK"
        },
        pwdSalt: "1234"
    },
    userProfileShop: {
        key: "shop",
        shoppingCart: "shoppingCart",
        shippingAddress: "shippingAddress",
    },
    paymentDetails: "Shop purchase for article id: ",
    currencyType: {
        money: "MONEY",
        bonus: "BONUS",
        credit: "CREDIT"
    }
};

var Database = require('nodejs-database').getInstance();
Config = _.assign(Config, Database.Config);

Config.wsURL = function (uri, testport) {
    var secure = !(process.env.TEST === 'true') && Config.secure;
    return 'ws' + (secure ? 's' : '') + '://' + Config.ip + ':' + Config.port + (uri ? uri : '/');
}

module.exports = Config;