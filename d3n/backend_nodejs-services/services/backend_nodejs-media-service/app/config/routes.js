var _ = require('lodash');
var cors = require('cors');
var HttpMediaApi = require('./../api/httpMediaApi.js');
var HttpDocumentApi = require('./../api/httpDocumentApi.js');
var WsMediaApi = require('./../api/wsMediaApi.js');

module.exports = function (service) {

    // HTTP routing
    if (_.has(service, 'http') && !_.isUndefined(service.http) && !_.isNull(service.http)) {
        service.http.options('/addMedia', cors());
        service.http.post('/addMedia',
          [cors(), service.httpUploadMiddleware.single('media_file')],
          HttpMediaApi.addMedia
        );

        service.http.options('/updateProfilePicture', cors());
        service.http.post('/updateProfilePicture',
          [cors(), service.httpUploadMiddleware.single('media_file')],
          HttpMediaApi.updateProfilePicture
        );

        service.http.options('/updateGroupPicture', cors());
        service.http.post('/updateGroupPicture',
          [cors(), service.httpUploadMiddleware.single('media_file')],
          HttpMediaApi.updateGroupPicture
        );

        service.http.options('/addDocument', cors());
        service.http.post('/addDocument',
          [cors(), service.httpUploadMiddleware.single('file_content')],
          HttpDocumentApi.addDocument
        );
    }

    // WS routing
    if (_.has(service, 'ws') && !_.isUndefined(service.ws) && !_.isNull(service.ws)) {
        _.each(_.functions(WsMediaApi), function (name) {
            service.ws.messageHandlers[name] = WsMediaApi[name];
        });
    }
};
