package de.ascendro.f4m.service.game.engine.client.profile;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.update.UpdateProfileRequest;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

import javax.inject.Inject;

public class ProfileCommunicator {

    private final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
    private final JsonMessageUtil jsonMessageUtil;
    private final ServiceRegistryClient serviceRegistryClient;

    @Inject
    public ProfileCommunicator(
            JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool,
            JsonMessageUtil jsonMessageUtil,
            ServiceRegistryClient serviceRegistryClient)
    {
        this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
        this.jsonMessageUtil = jsonMessageUtil;
        this.serviceRegistryClient = serviceRegistryClient;
    }

    public void requestUpdateProfileHandicap(
            String userId,
            String gameInstanceId,
            Double handicap,
            JsonMessage<? extends JsonMessageContent> sourceMessage,
            SessionWrapper session)
    {
        final UpdateProfileRequest request = new UpdateProfileRequest(userId);
        final Profile profile = new Profile();
        profile.setHandicap(handicap);
        request.setProfile(profile.getJsonObject());
        request.setService("gameEngineService");

        final JsonMessage<UpdateProfileRequest> message = jsonMessageUtil.createNewMessage(ProfileMessageTypes.UPDATE_PROFILE, request);
        final RequestInfo requestInfo = new ServiceRequestInfo(gameInstanceId);
        requestInfo.setSourceMessage(sourceMessage);
        requestInfo.setSourceSession(session);
        try {
            final ServiceConnectionInformation connInfo = serviceRegistryClient.getServiceConnectionInformation(ProfileMessageTypes.SERVICE_NAME);
            jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(connInfo, message, requestInfo);
        } catch (F4MValidationFailedException | F4MIOException e) {
            throw new F4MFatalErrorException("Unable to send update profile request to profile service", e);
        }
    }
}
