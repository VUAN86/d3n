var _ = require('lodash'),
    Config = require('./../../config/config.js'),
    Errors = require('./../../config/errors.js'),
    DataIds = require('./_id.data.js'),
    Database = require('./../../../index.js').getInstance(Config),
    DatabaseErrors = Database.Errors,
    RdbmsService = Database.RdbmsService,
    ConfigurationSetting = RdbmsService.Models.Configuration.Setting;

module.exports = {
    CONFIG_SETTING_1: {
        $id: ConfigurationSetting.constants().ID_F4M_LEGAL,
        data: '{ name: "image/png", termsAndConditions: "These are our terms and conditions"}'
    },

    load: function (testSet) {
        return testSet
            .createSeries(ConfigurationSetting, [this.CONFIG_SETTING_1]);
    }
};
