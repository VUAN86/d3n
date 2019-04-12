var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var TenantData = require('./../config/tenant.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeTenant = KeyvalueService.Models.AerospikeTenant;

describe('Tenant', function () {
    describe('Tenant AS check', function () {
        it('HAVE #remove', function () {
            AerospikeTenant.should.be.have.property('remove');
            AerospikeTenant.remove.should.be.a.Function;
        });
        it('HAVE #findOne', function () {
            AerospikeTenant.should.be.have.property('findOne');
            AerospikeTenant.findOne.should.be.a.Function;
        });
        it('HAVE #toJSON', function () {
            AerospikeTenant.should.be.have.property('toJSON');
            AerospikeTenant.toJSON.should.be.a.Function;
        });
        it('HAVE prototype #copy', function () {
            AerospikeTenant.prototype.should.be.have.property('copy');
            AerospikeTenant.prototype.copy.should.be.a.Function;
        });
        it('HAVE prototype #save', function () {
            AerospikeTenant.prototype.should.be.have.property('save');
            AerospikeTenant.prototype.save.should.be.a.Function;
        });
        it('HAVE prototype #remove', function () {
            AerospikeTenant.prototype.should.be.have.property('remove');
            AerospikeTenant.prototype.remove.should.be.a.Function;
        });
    });

    describe('Tenant add and remove operations', function () {
        it('test add + remove OK', function (done) {
            AerospikeTenant.update(TenantData.TENANT_1, function (err, tenant) {
                tenant.tenantId.should.eql(TenantData.TENANT_1.tenantId);
                tenant.apiId.should.eql(TenantData.TENANT_1.apiId);
                tenant.mainCurrency.should.eql(TenantData.TENANT_1.mainCurrency);
                AerospikeTenant.remove({ "tenantId": tenant.tenantId }, function (err, removedTenant) {
                    removedTenant.should.be.null;
                    if (err) return done(err);
                    done();
                });
            });
        });

        it('test update + remove OK', function (done) {
            AerospikeTenant.update(TenantData.TENANT_1, function (err, tenant) {
                var updatedTenant = _.cloneDeep(tenant);
                updatedTenant.apiId = 'test';
                AerospikeTenant.update(updatedTenant, function (err, updatedTenant) {
                    updatedTenant.apiId.should.eql('test');
                    AerospikeTenant.remove({ "tenantId": updatedTenant.tenantId }, function (err, removedTenant) {
                        removedTenant.should.be.null;
                        if (err) return done(err);
                        done();
                    });
                });
            });
        });
    });
});
