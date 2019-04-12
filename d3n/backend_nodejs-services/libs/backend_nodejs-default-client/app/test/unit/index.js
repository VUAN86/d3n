//process.env.VALIDATE_FULL_MESSAGE='false';
process.env.CHECK_CERT='false';
require('./wsServiceClient.test.js');
require('./wsAsynchronusMessagesAndErrors.test.js');
require('./wsTestMessagesQueue.test.js');
require('./wsNamespaces.test.js');
require('./wsTestFullMessageValidation.js');
require('./wsOnTheRoadMessage.test.js');

//require('./wsTestSecure.js');
//require('./wsHTTPProtocolLogging.test.js');