process.env.VALIDATE_FULL_MESSAGE='false';
process.env.CHECK_CERT='false';
require('./test-basic.js');
require('./test-resubscription.js');
