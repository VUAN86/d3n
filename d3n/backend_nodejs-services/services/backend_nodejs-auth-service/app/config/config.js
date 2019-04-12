var _ = require('lodash');
var fs = require('fs');
var path = require('path');
var S3Client = require('nodejs-s3client').getInstance();

var Config = {
    serviceName: 'auth',
    numCPUs: process.env.DEBUG ? 1 : require('os').cpus().length,
    ip: process.env.HOST || 'localhost',
    port: process.env.PORT || '9090',
    secure: true,
    key: fs.readFileSync(path.join(__dirname, '.', 'ssl-certificate/key.pem'), 'utf8'),
    cert: fs.readFileSync(path.join(__dirname, '.', 'ssl-certificate/cert.pem'), 'utf8'),
    registryServiceURIs: process.env.REGISTRY_SERVICE_URIS || 'ws://localhost:9093',
    validateFullMessage: _.isUndefined(process.env.VALIDATE_FULL_MESSAGE) ? true : (process.env.VALIDATE_FULL_MESSAGE === 'true'),
    umWaitForResponse: _.isUndefined(process.env.USER_MESSAGE_WAIT_FOR_RESPONSE) ? true : (process.env.USER_MESSAGE_WAIT_FOR_RESPONSE === 'true'),
    protocolLogging: _.isUndefined(process.env.PROTOCOL_LOGGING) ? false : (process.env.PROTOCOL_LOGGING === 'true'),
    gateway: {
        ip: process.env.HOST || 'localhost',
        port: (parseInt(process.env.PORT) + 9) || '9099',
        secure: true,
        key: path.join(__dirname, '.', 'ssl-certificate/key.pem'),
        cert: path.join(__dirname, '.', 'ssl-certificate/cert.pem'),
        auth: {
            algorithm: 'RS256',
            publicKey: fs.readFileSync(path.join(__dirname, '.', 'jwt-keys/pubkey.pem'), 'utf8'),
        },
    },
    auth: {
        algorithm: 'RS256',
        publicKey: fs.readFileSync(path.join(__dirname, '.', 'jwt-keys/pubkey.pem'), 'utf8'),
        privateKey: fs.readFileSync(path.join(__dirname, '.', 'jwt-keys/privkey.pem'), 'utf8')
    },
    fixedSalt: 'oewfgu9348u349erbuh89ghi3948bh9g84vhjnb934hg0j',
    blob: {
        cipher: 'aes192',
        bucket: S3Client.Config.buckets.medias.blob
    },
    jwt: {
        issuer: 'F4M Development and Testing',
        expiresInSeconds: 60 * 60 * 24 * 30 * 12 * 10, // 10 years = 10y * 12m * 30d * 24h * 60m * 60s
        impersonateForSeconds: 3600 // 1 hour 
    },
    codeConfirmation: {
        expiresInSeconds: 60 * 60 * 24 // 1 day
    },
    facebook: {
        url: 'https://graph.facebook.com/me',
        scope: 'public_profile',
        fields: ['token_for_business', 'email', 'name', 'first_name', 'last_name', 'birthday', 'link', 'gender', 'locale', 'picture', 'location{location{city,country_code}}', 'ids_for_business'],
        fieldsToRequest: true,
        fieldsUrl: null
    },
    google: {
        url: 'https://www.googleapis.com/userinfo/v2/me',
        scope: 'plus.profile.emails.read',
        fields: ['id', 'email'],
        fieldsToRequest: false,
        fieldsUrl: 'https://www.googleapis.com/plus/v1/people/me'
    },
    userMessage: {
        authChangePasswordEmail: {
            subject: 'auth.change.password.subject',
            message: 'auth.change.password.message',
        },
        authChangePasswordSms: {
            message: 'auth.change.password.sms',
        },
        authConfirmEmail: {
            subject: 'auth.confirm.email.subject',
            message: 'auth.confirm.email.message',
        },
        authConfirmPhone: {
            message: 'auth.confirm.phone.sms',
        },
        authConfirmEmailMessageWithPassword: {
            subject: 'auth.confirm.email.subject',
            message: 'auth.confirm.email.message.with.password.message',
        },
        authConfirmPhoneSmsWithPassword: {
            message: 'auth.confirm.phone.sms.with.password',
        },
        authSetUserRoleEmail: {
            subject: 'auth.set.user.role.subject',
            message: 'auth.set.user.role.message',
        },
        authSetUserRoleSms: {
            message: 'auth.set.user.role.sms',
        },
        authRecoverPasswordEmail: {
            subject: 'auth.recover.password.email.subject',
            message: 'auth.recover.password.email.message',
        },
        authRecoverPasswordConfirmEmail: {
            subject: 'auth.recover.password.confirm.email.subject',
            message: 'auth.recover.password.confirm.email.message',
        },
        authRecoverPasswordSms: {
            message: 'auth.recover.password.phone.sms',
        },
        authRecoverPasswordConfirmSms: {
            message: 'auth.recover.password.confirm.phone.sms',
        },
        authRegisterEmail: {
            subject: 'auth.register.email.subject',
            message: 'auth.register.email.message',
        },
        authRegisterPhone: {
            message: 'auth.register.phone.sms',
        },
        authRegisterFacebook: {
            subject: 'auth.register.facebook.subject',
            message: 'auth.register.facebook.message',
        },
        authRegisterGoogle: {
            subject: 'auth.register.google.subject',
            message: 'auth.register.google.message',
        },
        authRegisterEmailNewcode: {
            subject: 'auth.register.email.newcode.subject',
            message: 'auth.register.email.newcode.message',
        },
        authRegisterPhoneNewcode: {
            message: 'auth.register.phone.newcode.sms',
        },
        authInviteUserByEmail: {
            subject: 'auth.invite.user.by.email.subject',
            message: 'auth.invite.user.by.email.message',
        },
        authInviteUserByEmailAndRole: {
            subject: 'auth.invite.user.by.email.and.role.subject',
            message: 'auth.invite.user.by.email.and.role.message',
        },
    },
};

Config.httpURL = function (uri, testport) {
    var secure = !global.test && Config.secure;
    return 'http' + (secure ? 's' : '') + '://' + Config.ip + ':' + (testport ? testport : Config.port) + (uri ? uri : '/');
};

Config.wsURL = function (uri, testport) {
    var secure = !global.test && Config.secure;
    return 'ws' + (secure ? 's' : '') + '://' + Config.ip + ':' + (testport ? testport : Config.port) + (uri ? uri : '/');
};

module.exports = Config;