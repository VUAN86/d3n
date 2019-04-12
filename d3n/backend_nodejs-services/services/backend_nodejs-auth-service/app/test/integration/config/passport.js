var LocalStrategy = require('passport-local').Strategy,
    FacebookStrategy = require('passport-facebook').Strategy,
    GoogleStrategy = require('passport-google-oauth').OAuth2Strategy,
    Config = require('./../../../config/config.js'),
    PassportFactory = require('./../factories/passportFactory.js');

module.exports = function (passport) {
    
    passport.serializeUser(function (user, callback) {
        callback(null, user);
    });
    
    passport.deserializeUser(function (user, callback) {
        callback(null, user);
    });
    
    passport.use('local-login', new LocalStrategy({
        usernameField: 'username',
        passwordField: 'password',
        passReqToCallback: true
    }, PassportFactory.localLogin));
    
    passport.use(new FacebookStrategy({
        clientID: global.facebookAppId,
        clientSecret: global.facebookAppSecret,
        callbackURL: Config.httpURL('/auth/facebook/callback', global.testport),
        profileFields: [],
        passReqToCallback: true
    }, PassportFactory.facebookProfile));
    
    passport.use(new GoogleStrategy({
        clientID: global.googleAppId,
        clientSecret: global.googleAppSecret,
        callbackURL: Config.httpURL('/auth/google/callback', global.testport),
        profileFields: [],
        passReqToCallback: true
    }, PassportFactory.googleProfile));
    
};