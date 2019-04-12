var _ = require('lodash');
var fs = require('fs');
var path = require('path');
var S3Client = require('nodejs-s3client');

var Config = {
    serviceName: 'appConfigurator',
    serviceNamespaces: [
        'achievementManager',
        'advertisementManager',
        'application',
        'dummy',
        'game',
        'profileManager',
        'tenant',
        'tenantManagerConnector',
        'voucherManager',
        'promocodeManager',
        'tombolaManager',
        'winningManager',
    ],
    serviceApi: path.join(__dirname, '..', 'api'),
    numCPUs: process.env.DEBUG ? 1 : require('os').cpus().length,
    ip: process.env.HOST || 'localhost',
    port: process.env.PORT || '9090',
    secure: true,
    key: fs.readFileSync(path.join(__dirname,  'ssl-certificate/key.pem'), 'utf8'),
    cert: fs.readFileSync(path.join(__dirname,  'ssl-certificate/cert.pem'), 'utf8'),
    registryServiceURIs: process.env.REGISTRY_SERVICE_URIS || 'ws://localhost:9093',
    validateFullMessage: _.isUndefined(process.env.VALIDATE_FULL_MESSAGE) ? true : (process.env.VALIDATE_FULL_MESSAGE === 'true'),
    validatePermissions: _.isUndefined(process.env.VALIDATE_PERMISSIONS) ? false : (process.env.VALIDATE_PERMISSIONS === 'true'),
    umWaitForResponse: _.isUndefined(process.env.USER_MESSAGE_WAIT_FOR_RESPONSE) ? true : (process.env.USER_MESSAGE_WAIT_FOR_RESPONSE === 'true'),
    protocolLogging: _.isUndefined(process.env.PROTOCOL_LOGGING) ? false : (process.env.PROTOCOL_LOGGING === 'true'),
    schedulerServiceEnabled: _.isUndefined(process.env.SCHEDULER_SERVICE_ENABLED) ? true : (process.env.SCHEDULER_SERVICE_ENABLED === 'true'),
    backgroundProcesses: {
        // App Configurator service jobs
        jobs: path.join(__dirname, '../', 'jobs'),
        // If true, run background processing servise and load job instances from CronMasterJob (common jobs, app/jobs/*.js) classes
        enabled: true,
        // If true, run background processing servise as Node Child Process (currently not supported)
        asChildProcess: false,
        // If true, run GC explicitly on TICK_STARTED, prints out differences to console on TICK_COMPLETE - may take extra 20 seconds to process
        jobHeapDirrerences: false,
        // If true, exports heap snapshot first on TICK_STARTED, then second on TICK_COMPLETE
        jobHeapSnapshots: false,
        // Voucher threshold limit, under which admins will be signalized that voucher soon will reach it's limits and marked as used
        voucherCheckThresholdLimit: 100,
    },
    memoryWatching: {
        // If true, run memory watching servise, which prints out GC state on change (state event) and monitors possible memory leaks (leak event) and then prints out heap snapshots
        enabled: false,
    },
    elastic: {
        username: _.isUndefined(process.env.ELASTIC_USERNAME) ? '' : process.env.ELASTIC_USERNAME,
        password: _.isUndefined(process.env.ELASTIC_PASSWORD) ? '' : process.env.ELASTIC_PASSWORD,
        server: _.isUndefined(process.env.ELASTIC_SERVER) ? 'search-f4mtesting-qtoyj5iivyi5uvwkxuiwowvlna.eu-central-1.es.amazonaws.com' : process.env.ELASTIC_SERVER,
        port: _.isUndefined(process.env.ELASTIC_PORT) ? '' : process.env.ELASTIC_PORT
    },
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
    tenantManager: {
        // Security settings for the HTTPS tenant manager service
        username: _.isUndefined(process.env.TENANT_MANAGER_USERNAME) ? 'admin' : process.env.TENANT_MANAGER_USERNAME,
        password: _.isUndefined(process.env.TENANT_MANAGER_PASSWORD) ? 's3cr3t' : proces.env.TENANT_MANAGER_PASSWORD,
        // Allow access to the service from this IPv4 address, by default allow from all.
        clientIP: _.split(_.isUndefined(process.env.TENANT_MANAGER_ALLOWED_IP_ADDR) ? '127.0.0.1,81.196.178.147' : process.env.TENANT_MANAGER_ALLOWED_IP_ADDR, ',')
    },
    blob: {
        cipher: 'aes192',
        bucket: process.env.BLOB_BUCKET || S3Client.Config.buckets.appConfigurator.blob
    },
    game: {
        minimumQuestions: _.isUndefined(process.env.GAME_MIN_QUESTION) ? 100 : parseInt(process.env.GAME_MIN_QUESTION),
    },
    advertisementPublish: {
        bucket: process.env.ADVERTISEMENT_BUCKET || S3Client.Config.buckets.appConfigurator.advertisement
    },
    appConfigBucket: process.env.APP_CONFIG_BUCKET || S3Client.Config.buckets.medias.medias,
    userMessage: {
        gameInvalidDeactivatedEmail: {
            subject: 'game.invalid.deactivated.subject',
            message: 'game.invalid.deactivated.message'
        },
        voucherThresholdReachingEmail: {
            subject: 'voucher.threshold.reaching.subject',
            message: 'voucher.threshold.reaching.message'
        },
        profileManagerProfileSyncGetEmail: {
            subject: 'profileManager.profileSync.get.subject',
            message: 'profileManager.profileSync.get.message'
        },
        profileManagerProfileSyncCheckEmail: {
            subject: 'profileManager.profileSync.check.subject',
            message: 'profileManager.profileSync.check.message'
        },
        tenantCheckMoney: {
            subject: 'Tenant balance validation'
        }
    },
    f4mLegalEntities: {
        /*
         * This static configuration data can be overriden in the database 
         * configuration_setting table.
         * Values defined in the MySQL table extend this static configuration.
         * See tenantService.retrieveF4mLegalEntities for more information.
         */
        name: 'friends4media GmbH',
        address: 'Waffnergasse 8',
        city: '93047 Regensburg',
        country: 'Germany',
        vat: 'DE295946616',
        url: 'http://www.f4m.tv/',
        email: 'kontakt@f4m.tv',
        description: fs.readFileSync(path.join(__dirname,  'f4mDescription.txt'), 'utf8'),
        termsAndConditions: fs.readFileSync(path.join(__dirname,  'termsAndConditions.txt'), 'utf8'),
        privacyStatement: fs.readFileSync(path.join(__dirname,  'privacyStatement.txt'), 'utf8'),
        gameRules: 'F4M Game Rules',
        copyright: 'F4M Copyright',
        phone: '12341234',
        logoUrl: 'http://www.f4m.tv/wp-content/themes/darbas/dist/images/logo.png'
    },
    urlMediaService: '',
    
    defaultPreCloseOffsetMinutes: 5,
    
    fyber: {
        clientSecurityToken: process.env.FYBER_CLIENT_SECURITY_TOKEN || '',
        rewardHandlingSecurityToken: process.env.FYBER_REWARD_SECURITY_TOKEN || '',
        appId: process.env.FYBER_APP_ID || ''
    },
    
    defaultRepeatCount: 400
};

var Database = require('nodejs-database').getInstance();
Config = _.assign(Config, Database.Config);

module.exports = Config;

Config.httpURL = function (uri, testport) {
    var secure = !(process.env.TEST === 'true') && Config.secure;
    return 'http' + (secure ? 's' : '') + '://' + Config.ip + ':' + Config.port + (uri ? uri : '/');
}

Config.wsURL = function (uri, testport) {
    var secure = !(process.env.TEST === 'true') && Config.secure;
    return 'ws' + (secure ? 's' : '') + '://' + Config.ip + ':' + Config.port + (uri ? uri : '/');
}

Config.getHRTimestamp = function () {
    var date = new Date();
    var month = date.getMonth() + 1;
    var day = date.getDate();
    var hour = date.getHours();
    var min = date.getMinutes();
    var sec = date.getSeconds();

    month = (month < 10 ? '0' : '') + month;
    day = (day < 10 ? '0' : '') + day;
    hour = (hour < 10 ? '0' : '') + hour;
    min = (min < 10 ? '0' : '') + min;
    sec = (sec < 10 ? '0' : '') + sec;

    var str = date.getFullYear() + '-' + month + '-' + day + '-' + hour + '-' + min + '-' + sec + '-' + date.getMilliseconds();
    return str;
}

var urlMediaServices = {
    'f4m-dev-': 'https://media-dev.dev4m.com/',
    'f4m-nightly-': 'https://media-nightly.dev4m.com/',
    'f4m-staging-': 'https://media-staging.dev4m.com/',
    'f4m-': 'https://media.f4mgamesys.com/'
};

Config.urlMediaService = _.isUndefined(process.env.PUBLIC_MEDIA_SERVICE_DOMAIN) ? urlMediaServices[process.env.S3_API_BUCKET_PREFIX] : process.env.PUBLIC_MEDIA_SERVICE_DOMAIN;
