package de.ascendro.f4m.service.handler;

import de.ascendro.f4m.service.json.handler.JsonAuthenticationMessageMQHandler;

public interface MessageHandler {
    JsonAuthenticationMessageMQHandler createServiceMessageHandler();
}
