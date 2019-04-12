package de.ascendro.f4m.service.winning.client;

import javax.inject.Inject;

import de.ascendro.f4m.service.communicator.RabbitClientSender;
import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;
import de.ascendro.f4m.service.winning.config.WinningConfig;
import de.ascendro.f4m.service.winning.model.WinningComponent;

public class UserMessageServiceCommunicator {
    private WinningConfig config;
    private JsonMessageUtil jsonUtil;
//    private JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
//    private ServiceRegistryClient serviceRegistryClient;

    @Inject
    public UserMessageServiceCommunicator(WinningConfig config, JsonMessageUtil jsonUtil) {
//                                          JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, ServiceRegistryClient serviceRegistryClient
        this.config = config;
        this.jsonUtil = jsonUtil;
//        this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
//        this.serviceRegistryClient = serviceRegistryClient;
    }

    public void notifyAdministratorInsufficientTenantFunds(String userId, WinningComponent winningComponent, String tenantId) {
        String emailAddress = config.getProperty(WinningConfig.ADMIN_EMAIL);
        if (StringUtils.isNotBlank(emailAddress)) {
            SendEmailWrapperRequest request = new SendEmailWrapperRequest();
            request.setAddress(emailAddress);
            //no params necessary since message is already prepared and no translation is necessary for emails to admin
            request.setSubject(
                    "Insufficient tenant funds to payout winning in winning service on "
                            +  config.getProperty(WinningConfig.SERVICE_HOST) + ":"
                            + config.getProperty(WinningConfig.JETTY_SSL_PORT));
            request.setMessage("For userId " + userId + " there is not enough money in paydent to pay out winning component "
                    + winningComponent.getWinningComponentId() + " won by " + userId);
            request.setISOLanguage(ISOLanguage.EN); //use english to have email header/footer translated
            JsonMessage<SendEmailWrapperRequest> requestJson = jsonUtil
                    .createNewMessage(UserMessageMessageTypes.SEND_EMAIL, request);
            try {
                RabbitClientSender.sendAsyncMessage(UserMessageMessageTypes.SERVICE_NAME, requestJson);
            } catch (F4MValidationFailedException | F4MIOException e) {
                throw new F4MFatalErrorException("Unable to send sendEmail request to user message service", e);
            }
        }
    }
}
