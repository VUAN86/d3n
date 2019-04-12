var _ = require('lodash');
var yargs = require('yargs');
var Config = require('./app/config/config.js');
var Errors = require('./app/config/errors.js');
var RdbmsService = require('./app/services/rdbmsService.js');
var TestService = require('./app/services/testService.js');
var logger = require('nodejs-logger')();
var cp = require('child_process');

var cmd = '../../node_modules/.bin/sequelize';
if (process.platform === 'win32') {
    cmd = '..\..\node_modules\.bin\sequelize.cmd';
}

var Database = function (config) {
    this._config = _.assign(Config, config);
    Database.prototype.RdbmsService = RdbmsService.getInstance(this._config);
    Database.prototype.TestService = TestService.getInstance(this);
    Database.prototype.Config = this._config;
    Database.prototype.Errors = Errors;
}

Database.getInstance = function (config) {
    if (_.isUndefined(Database._instance)) {
        Database._instance = new Database(config);
    }
    return Database._instance;
}

module.exports = Database;

var argp = yargs
    .usage('$0 [options]')
    .options({
        'rdbms-sync': {
            boolean: true,
            describe: 'Synchronize relational database schema.'
        },
        'rdbms-migrate': {
            boolean: true,
            describe: 'Execute migrations against relational database schema.'
        },
        'rdbms-migrate-undo': {
            boolean: true,
            describe: 'Revert the last migration run.'
        },
        
        'rdbms-migrate-undo-all': {
            boolean: true,
            describe: 'Revert all migrations ran'
        }
    });
var argv = argp.argv;

if (argv.rdbmsSync === true) {
    var database = Database.getInstance();
    database.RdbmsService.sync({
        logging: database.Config.rdbms.logging,
        force: database.Config.rdbms.force
    }, function (err) {
        if (err) {
            logger.error('Error synchronizing relational database schema: ', err);
            process.exit(1);
        }
        logger.info('Relational database schema synchronized successfully');
        process.exit(0);
    });
}

if (argv.rdbmsMigrate === true) {
    var database = Database.getInstance();
    database.RdbmsService.sync({
        logging: database.Config.rdbms.logging,
        force: false
    }, function (err) {
        if (err) {
            logger.error('Error synchronizing relational database schema: ', err);
            process.exit(1);
        }
        logger.info('Relational database schema synchronized successfully');
        
        var args = ['--config=app/config/config-migration.js', '--migrations-path=app/migrations', 'db:migrate'];
        var result = cp.spawnSync(cmd, args);
        
        if (parseInt(result.status) === 0) {
            logger.info('Relational database migration successfully');
            process.exit(0);
        } else {
            if (process.platform === 'win32') {
                logger.error('Error on migration');
            } else {
                logger.error('Error on migration:', result.stdout.toString(), result.stderr.toString());
            }
            process.exit(1);
        }
    });
}

if (argv.rdbmsMigrateUndo === true) {
    var args = ['--config=app/config/config-migration.js', '--migrations-path=app/migrations', 'db:migrate:undo'];
    var result = cp.spawnSync(cmd, args);

    if (parseInt(result.status) === 0) {
        logger.info('Relational database migration undo successfully');
        process.exit(0);
    } else {
        if (process.platform === 'win32') {
            logger.error('Error on migration undo');
        } else {
            logger.error('Error on migration undo:', result.stdout.toString(), result.stderr.toString());
        }
        process.exit(1);
    }
}

if (argv.rdbmsMigrateUndoAll === true) {
    var args = ['--config=app/config/config-migration.js', '--migrations-path=app/migrations', 'db:migrate:undo:all'];
    var result = cp.spawnSync(cmd, args);

    if (parseInt(result.status) === 0) {
        logger.info('Relational database migration undo all successfully');
        process.exit(0);
    } else {
        if (process.platform === 'win32') {
            logger.error('Error on migration undo all');
        } else {
            logger.error('Error on migration undo all:', result.stdout.toString(), result.stderr.toString());
        }
        process.exit(1);
    }
}