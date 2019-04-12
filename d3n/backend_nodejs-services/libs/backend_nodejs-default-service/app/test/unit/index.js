//process.env.VALIDATE_FULL_MESSAGE='false';
process.env.CHECK_CERT='false';
require('./test-direct');
require('./test-direct-insecure');
require('./test-broadcast-system');
require('./test-data-storage-system');
require('./test-event-emitter');
require('./test-namespaces');
require('./test-full-message-validation');
require('./test-newrelic.js');
require('./test-statistics-monitor.js');
require('./test-send-statistics.js');
require('./test-cert-validation.js');






//require('./test-heartbeat');
//require('./test-gateway');
//require('./test-httpprotocollogger');


