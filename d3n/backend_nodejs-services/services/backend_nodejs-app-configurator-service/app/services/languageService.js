var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var Database = require('nodejs-database').getInstance(Config);
var RegionalSetting = Database.RdbmsService.Models.Question.RegionalSetting;
var RegionalSettingHasLanguage = Database.RdbmsService.Models.Question.RegionalSettingHasLanguage;
var Language = Database.RdbmsService.Models.Question.Language;

module.exports = {

    regionalSettingGet: function (params, callback) {
        try {
            var include = CrudHelper.include(RegionalSetting, [
                RegionalSettingHasLanguage
            ]);
            return RegionalSetting.findOne({
                where: { id: params.id },
                include: include
            }).then(function (regionalSetting) {
                if (!regionalSetting) {
                    return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                }
                var regionalSettingItem = regionalSetting.get({ plain: true });
                return CrudHelper.callbackSuccess({ regionalSetting: regionalSettingItem }, callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    regionalSettingGetByIso: function (params, callback) {
        try {
            var include = CrudHelper.include(RegionalSetting, [
                RegionalSettingHasLanguage
            ]);
            return RegionalSetting.findOne({
                where: { iso: params.iso },
                include: include
            }).then(function (regionalSetting) {
                if (!regionalSetting) {
                    return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                }
                var regionalSettingItem = regionalSetting.get({ plain: true });
                return CrudHelper.callbackSuccess({ regionalSetting: regionalSettingItem }, callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    languageGet: function (params, callback) {
        try {
            return Language.findOne({
                where: { id: params.id }
            }).then(function (language) {
                if (!language) {
                    return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                }
                var languageItem = language.get({ plain: true });
                return CrudHelper.callbackSuccess({ language: languageItem }, callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    languageGetByIso: function (params, callback) {
        try {
            return Language.findOne({
                where: { iso: params.iso }
            }).then(function (language) {
                if (!language) {
                    return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                }
                var languageItem = language.get({ plain: true });
                return CrudHelper.callbackSuccess({ language: languageItem }, callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

};