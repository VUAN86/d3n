var _ = require('lodash');

var AerospikeUser = function AerospikeUser() {};

AerospikeUser.ROLE_ANONYMOUS = 'ANONYMOUS';
AerospikeUser.ROLE_NOT_VALIDATED = 'NOT_VALIDATED';
AerospikeUser.ROLE_REGISTERED = 'REGISTERED';
AerospikeUser.ROLE_FULLY_REGISTERED = 'FULLY_REGISTERED';
AerospikeUser.ROLE_FULLY_REGISTERED_BANK = 'FULLY_REGISTERED_BANK';
AerospikeUser.ROLE_FULLY_REGISTERED_BANK_O18 = 'FULLY_REGISTERED_BANK_O18';
AerospikeUser.ROLE_COMMUNITY = 'COMMUNITY';
AerospikeUser.ROLE_ADMIN = 'ADMIN';
AerospikeUser.ROLE_INTERNAL = 'INTERNAL';
AerospikeUser.ROLE_EXTERNAL = 'EXTERNAL';

AerospikeUser.INTERNAL_PASSWORD = '_int3rnal$';

module.exports = AerospikeUser;