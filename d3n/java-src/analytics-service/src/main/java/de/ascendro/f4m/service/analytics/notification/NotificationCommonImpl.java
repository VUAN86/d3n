package de.ascendro.f4m.service.analytics.notification;

import javax.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.server.analytics.exception.F4MAnalyticsFatalErrorException;
import de.ascendro.f4m.service.analytics.config.AnalyticsConfig;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageRequest;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class NotificationCommonImpl implements NotificationCommon {
    protected final Config config;
    protected final JsonMessageUtil jsonMessageUtil;
    protected final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
    protected final ServiceRegistryClient serviceRegistryClient;

    @Inject
    public NotificationCommonImpl(Config config, JsonMessageUtil jsonMessageUtil,
                                  JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, ServiceRegistryClient serviceRegistryClient) {
        this.config = config;
        this.jsonMessageUtil = jsonMessageUtil;
        this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
        this.serviceRegistryClient = serviceRegistryClient;
    }


    @Override
    public void sendEmailToAdmin(String subject, String[] subjectParameters, String body, String[] bodyParameters) {
        SendEmailWrapperRequest request = new SendEmailWrapperRequest();
        request.setAddress(config.getProperty(AnalyticsConfig.ADMIN_EMAIL));
        request.setSubject(subject);
        request.setSubjectParameters(subjectParameters);
        request.setMessage(body);
        request.setParameters(bodyParameters);
        request.setISOLanguage(ISOLanguage.EN);
        JsonMessage<SendEmailWrapperRequest> requestJson = jsonMessageUtil.createNewMessage(
                UserMessageMessageTypes.SEND_EMAIL, request);
        try {
            jsonWebSocketClientSessionPool.sendAsyncMessage(
                    serviceRegistryClient.getServiceConnectionInformation(UserMessageMessageTypes.SERVICE_NAME),
                    requestJson);
        } catch (F4MValidationFailedException | F4MIOException e) {
            throw new F4MAnalyticsFatalErrorException("Unable to send sendEmail request to user message service", e);
        }
    }

    @Override
    public void sendEmailToUser(JsonMessage<?> message, SessionWrapper sessionWrapper, String toUserId, String subject,
                                String content, String fromUser) {
        SendEmailWrapperRequest request = new SendEmailWrapperRequest();
        request.setUserId(toUserId);
        request.setSubject(subject);
        request.setMessage(content);
        request.setParameters(new String[]{fromUser});
        request.setLanguageAuto();
        JsonMessage<SendEmailWrapperRequest> requestJson = jsonMessageUtil.createNewMessage(
                UserMessageMessageTypes.SEND_EMAIL, request);
        RequestInfo requestInfo = new RequestInfoImpl(message);
        requestInfo.setSourceSession(sessionWrapper);
        try {
            jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(
                    serviceRegistryClient.getServiceConnectionInformation(UserMessageMessageTypes.SERVICE_NAME), requestJson, requestInfo);
        } catch (F4MValidationFailedException | F4MIOException e) {
            throw new F4MFatalErrorException("Unable to send sendEmail request", e);
        }
    }

    @Override
    public void pushMessageToUser(String userId, String messageText, String[] params, ClientInfo clientInfo) {
        SendWebsocketMessageRequest requestContent = new SendWebsocketMessageRequest(true);
        requestContent.setUserId(userId);
        requestContent.setMessage(messageText);
        if (params != null) {
            requestContent.setParameters(params);
        }
        
        JsonMessage<SendWebsocketMessageRequest> message = jsonMessageUtil
                .createNewMessage(UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE, requestContent);
        
        message.setClientInfo(clientInfo);

        try {
            ServiceConnectionInformation userMessageConnInfo = serviceRegistryClient.getServiceConnectionInformation(UserMessageMessageTypes.SERVICE_NAME);
            jsonWebSocketClientSessionPool.sendAsyncMessage(userMessageConnInfo, message);
        } catch (F4MValidationFailedException | F4MIOException e) {
            throw new F4MFatalErrorException("Unable to send SendMobilePushRequest to User Message Service", e);
        }
    }
}
