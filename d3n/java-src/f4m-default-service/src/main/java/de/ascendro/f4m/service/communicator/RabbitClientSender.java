package de.ascendro.f4m.service.communicator;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessageError;
import de.ascendro.f4m.service.json.model.MessageSource;
import de.ascendro.f4m.service.request.RequestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Hashtable;
import java.util.concurrent.TimeoutException;

public class RabbitClientSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitClientSender.class);
    private static JsonMessageUtil jsonUtil;
    private static Config config;
    private static Connection connection;
    private static Channel channel;
    private static String callbackQueue;
    private static Hashtable<Long, MessageSource> requestsPool= new Hashtable();


    public static void init(JsonMessageUtil jsonUtil, Config config) {
        RabbitClientSender.jsonUtil = jsonUtil;
        RabbitClientSender.config = config;
        initConnection();
    }

    private static void initConnection() {
        try {
            RabbitClientSender.connection = RabbitClientConfig.factory.newConnection();
            RabbitClientSender.channel = connection.createChannel();
        } catch (TimeoutException | IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized static MessageSource getOriginalRequest(Long seq){
        return requestsPool.get(seq);
    }

    public synchronized static void addRequest(Long seq, MessageSource messageSource){
        if (requestsPool.contains(seq)){
            LOGGER.error("Requestwith seq={} as alredy exist! message={}",seq,messageSource.getOriginalRequestInfo());
            System.out.println("Requestwith seq={"+seq+"} as alredy exist! message={"+messageSource.getOriginalRequestInfo());
        } else {
            requestsPool.put(seq,messageSource);
            System.out.println("addedRequest:"+seq+":"+messageSource);
        }
    }




    /*          ERROR   */
    public static void sendErrorMessage(String routingKey, JsonMessage<? extends JsonMessageContent> originalMessageDecoded, Throwable e) {
        System.out.println("sendErrorMessage routingKey=" + routingKey);
        sendAsyncMessage(routingKey, jsonUtil.createResponseErrorMessage(
                (e instanceof F4MException) ? (F4MException) e : new F4MFatalErrorException("Failed to process message", e)
                , originalMessageDecoded));
    }

    public static void sendErrorMessage(String routingKey, JsonMessage<? extends JsonMessageContent> originalMessageDecoded,
                                 JsonMessageError error) {
        sendAsyncMessage(routingKey, jsonUtil.createResponseErrorMessage(error, originalMessageDecoded));
    }


    public static void sendAsyncMessageWithClientInfo(String routingKey, JsonMessage<? extends JsonMessageContent> message, RequestInfo requestInfo) {
        sendAsyncMessage(routingKey, message, requestInfo, true);
    }

    public static void sendAsyncMessageNoClientInfo(String routingKey, JsonMessage<? extends JsonMessageContent> message, RequestInfo requestInfo) {
        sendAsyncMessage(routingKey, message, requestInfo, false);
    }

    public static void sendAsyncMessage(String routingKey, JsonMessage<? extends JsonMessageContent> message, RequestInfo requestInfo, boolean forwardClientInfo) {
//        final SessionWrapper session = getSession(serviceConnectionInformation);
//        registerRequestInfo(session, requestInfo, message.getSeq());
        MessageSource sourceMessageSource = requestInfo.getSourceMessageSource();
        MessageSource messageSource = new MessageSource(sourceMessageSource.getOriginalQueue(),requestInfo,sourceMessageSource.getSeq());
        RabbitClientSender.addRequest(message.getSeq(),messageSource);
        sendAsynMessage(routingKey, message, requestInfo, forwardClientInfo);
    }

    //                                                                                                              _________
    //    public static  void sendAsynMessage(JsonMessage<? extends JsonMessageContent> message)               ____|         |
    public static void sendAsyncMessage(String routingKey, JsonMessage<? extends JsonMessageContent> message)//            <-|
            throws F4MValidationFailedException {
        RabbitClientSender.sendAsynText(routingKey, jsonUtil.toJson(message));
    }


    public static void sendAsynMessage(String routingKey, JsonMessage<? extends JsonMessageContent> message, RequestInfo requestInfo, boolean forwardClientInfo) {
//        final ForwardedMessageSendHandler forwardedMessageSendHandler = createForwardedMessageSendHandler(message, requestInfo);
        if (forwardClientInfo) {
            copyClientInfo(requestInfo.getSourceMessage(), message);
        }
//        getSessionStore().registerRequest(message.getSeq(), requestInfo);
//        sendAsynText(routingKey,jsonUtil.toJson(message), forwardedMessageSendHandler);
        RabbitClientSender.sendAsynText(routingKey, jsonUtil.toJson(message));
    }



    public static void sendAsynText(String routingKey, String messageBody) {

        assert validateJson(messageBody);
//        int textLength = StringUtils.length(messageBody);
//        if (textLength >= session.getMaxTextMessageBufferSize()) {
//            //adding more elaborate message for error being thrown by org.eclipse.jetty.websocket.api.WebSocketPolicy.assertValidTextMessageSize
//            LOGGER.error("Cannot send message too large - expected max size {} but was {} {}",
//                    session.getMaxTextMessageBufferSize(), textLength, messageBody);
//        }

        try {

            byte[] messageBodyBytes = (messageBody).getBytes();
            boolean isCallback = routingKey.contains("Callback");
            channel.basicPublish(isCallback ? "" : RabbitClientConfig.exchangeName, routingKey, new AMQP.BasicProperties.Builder()
                    .replyTo(callbackQueue)
                    .contentType("text/plain")
                    .build(), messageBodyBytes);
            LOGGER.debug("SEND message {} to {}", messageBody, routingKey);
            System.out.println("SEND " + messageBody + " to " + routingKey);


        } catch (IOException e) {
            LOGGER.error("sendAsynText error: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    protected static void copyClientInfo(JsonMessage<?> sourceMessage, JsonMessage<?> targetMessage) {
        if (sourceMessage != null && targetMessage.getClientInfo() == null && sourceMessage.getClientInfo() != null) {
            targetMessage.setClientInfo(sourceMessage.getClientInfo());
        } else if (targetMessage.getClientInfo() != null) {
            LOGGER.warn("Target json message already has client info specified set: [{}]", targetMessage);
        } else if (sourceMessage != null && sourceMessage.getClientInfo() == null) {
            LOGGER.debug("Source json message is missing client info: [{}]", sourceMessage);
        }
    }

    private static boolean validateJson(String text) {
        jsonUtil.validate(text);
        return true;
    }

    public static void setCallbackQueue(String callbackQueue) {
        RabbitClientSender.callbackQueue = callbackQueue;
    }
}
