package de.ascendro.f4m.service.communicator;

import de.ascendro.f4m.service.handler.ServiceMessageMQHandler;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.MessageSource;

public class MessageThread extends Thread{
    ServiceMessageMQHandler<RequestContext, String, JsonMessageContent, MessageSource> messageHandler;
    MessageSource messageSource;
    String message;

    public MessageThread(ServiceMessageMQHandler<RequestContext, String, JsonMessageContent, MessageSource> messageHandler,
                         String message,
                         MessageSource messageSource) {
        this.messageHandler = messageHandler;
        this.messageSource = messageSource;
        this.message = message;
    }


    @Override
    public void run() {
        messageHandler.onMessage(message, messageSource);
        super.run();
    }
}
