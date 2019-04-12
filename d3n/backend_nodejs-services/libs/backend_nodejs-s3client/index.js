var Config = require('./app/config/config.js');
var S3Storage = require('./app/factories/S3Storage.js');

var S3Client = function (config) {
    this._config = config ? config : Config;
    S3Client.prototype.Storage = new S3Storage(this._config.amazon_s3);
    S3Client.prototype.SecureStorage = new S3Storage(this._config.secure_s3);
    S3Client.prototype.Config = this._config;
}

S3Client.Config = Config;

S3Client.getInstance = function (config) {
    return new S3Client(config);
}

module.exports = S3Client;
