var Config = require('./config.js');

module.exports = {
    development: {
        username: Config.rdbms.username,
        password: Config.rdbms.password,
        database: Config.rdbms.schema,
        host: Config.rdbms.host,
        port: Config.rdbms.port,
        dialect: 'mysql',
        migrationStorage: 'sequelize',
        migrationStorageTableName: '__migrations__'
    },
    production : {
        username: Config.rdbms.username,
        password: Config.rdbms.password,
        database: Config.rdbms.schema,
        host: Config.rdbms.host,
        port: Config.rdbms.port,
        dialect: 'mysql',
        migrationStorage: 'sequelize',
        migrationStorageTableName: '__migrations__'
    }
};