var express = require('express');
var session = require('express-session');
var bodyParser = require('body-parser');
var cookieParser = require('cookie-parser');
var morgan = require('morgan');
var flash = require('connect-flash');
var yargs = require('yargs');
var Config = require('./../../config/config.js');
var logger = require('nodejs-logger')();

var argp = yargs
    .usage('$0 [options]')
    .options({
        facebookAppId: {
            type: 'string',
            demand: true,
            describe: 'Facebook Test Application ID.'
        },
        facebookAppSecret: {
            type: 'string',
            demand: true,
            describe: 'Facebook Test Application Secret.'
        },
        googleAppId: {
            type: 'string',
            demand: true,
            describe: 'Google Test Application ID.'
        },
        googleAppSecret: {
            type: 'string',
            demand: true,
            describe: 'Google Test Application Secret.'
        },
    });
var argv = argp.argv;

global.facebookAppId = argv.facebookAppId;
global.facebookAppSecret = argv.facebookAppSecret;
global.googleAppId = argv.googleAppId;
global.googleAppSecret = argv.googleAppSecret;
global.testport = parseInt(Config.port) + 5;

var app = express();

var passport = require('passport');
require('./config/passport.js')(passport);

app.use(morgan('dev'));
app.use(cookieParser());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({
    extended: true
}));
app.use(session({
    secret: 'oauth2secret',
    saveUninitialized: true,
    resave: true
}));

app.use(passport.initialize());
app.use(passport.session());
app.use(flash());

app.set('views', process.cwd() + '/app/test/integration/views');
app.set('view engine', 'ejs');

require('./config/routes.js')(app, passport);

app.listen(global.testport);
logger.info('Integration Test Server running on port: ' + global.testport);
