package de.ascendro.f4m.service.game.selection.client.communicator;

import javax.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.selection.request.FriendsGameListRequestInfoImpl;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.profile.model.sub.get.GetProfileBlobRequest;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class ProfileCommunicator {

	private static final String PROFILE_GAMES_SUB_BLOB_NAME = "games";

	private final ServiceRegistryClient serviceRegistryClient;
	private final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private final JsonMessageUtil jsonMessageUtil;

	@Inject
	public ProfileCommunicator(ServiceRegistryClient serviceRegistryClient,
			JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, JsonMessageUtil jsonMessageUtil) {
		this.serviceRegistryClient = serviceRegistryClient;
		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
		this.jsonMessageUtil = jsonMessageUtil;
	}

	public void requestGetProfileBlob(FriendsGameListRequestInfoImpl requestInfo) {
		GetProfileBlobRequest requestContent = new GetProfileBlobRequest();
		requestContent.setUserId(requestInfo.getNextFriend());
		requestContent.setName(PROFILE_GAMES_SUB_BLOB_NAME);
		JsonMessage<GetProfileBlobRequest> message = jsonMessageUtil.createNewMessage(ProfileMessageTypes.GET_PROFILE_BLOB, requestContent);

		try {
			final ServiceConnectionInformation profileManagerConnInfo = serviceRegistryClient
					.getServiceConnectionInformation(ProfileMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(profileManagerConnInfo, message, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send GetProfileBlobRequest to Profile Service", e);
		}
	}

}
