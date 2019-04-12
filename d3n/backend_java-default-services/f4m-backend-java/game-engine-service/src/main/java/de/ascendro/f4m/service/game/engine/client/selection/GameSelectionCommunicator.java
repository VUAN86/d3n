package de.ascendro.f4m.service.game.engine.client.selection;

import javax.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.selection.GameSelectionMessageTypes;
import de.ascendro.f4m.service.game.selection.model.dashboard.PlayedGameInfo;
import de.ascendro.f4m.service.game.selection.model.dashboard.UpdatePlayedGameRequest;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.ActivateInvitationsRequest;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class GameSelectionCommunicator {

	private final JsonMessageUtil jsonMessageUtil;
	private final ServiceRegistryClient serviceRegistryClient;
	private final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;

	@Inject
	public GameSelectionCommunicator(JsonMessageUtil jsonMessageUtil, ServiceRegistryClient serviceRegistryClient,
			JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool) {
		this.jsonMessageUtil = jsonMessageUtil;
		this.serviceRegistryClient = serviceRegistryClient;
		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
	}

	/** Update played game */
	public void requestUpdatePlayedGame(ClientInfo clientInfo, String gameId, GameType gameType, String title) {
		PlayedGameInfo playedGameInfo = new PlayedGameInfo(gameId, gameType, title, false);

		UpdatePlayedGameRequest content = new UpdatePlayedGameRequest(clientInfo.getTenantId(), clientInfo.getUserId(), playedGameInfo);
		sendUpdatePlayedGameRequest(content);
	}
	
	/** Update finished multiplayer game */
	public void requestUpdatePlayedGame(String tenantId, String mgiId) {
		UpdatePlayedGameRequest content = new UpdatePlayedGameRequest(tenantId, mgiId);
		sendUpdatePlayedGameRequest(content);
	}

	private void sendUpdatePlayedGameRequest(UpdatePlayedGameRequest content) {
		JsonMessage<UpdatePlayedGameRequest> message = jsonMessageUtil.createNewMessage(GameSelectionMessageTypes.UPDATE_PLAYED_GAME, content);
		sendRequest(message);
	}

	/**
	 * Activate invitations in state PENDING
	 * @param mgiId
	 * @param sourceClientInfo 
	 */
	public void requestActivateInvitations(String mgiId, ClientInfo sourceClientInfo) {
		ActivateInvitationsRequest content = new ActivateInvitationsRequest(mgiId);
		JsonMessage<ActivateInvitationsRequest> message = jsonMessageUtil.createNewMessage(GameSelectionMessageTypes.ACTIVATE_INVITATIONS, content);
		message.setClientInfo(sourceClientInfo);
		sendRequest(message);
	}

	private void sendRequest(JsonMessage<? extends JsonMessageContent> message) {
		try {
			ServiceConnectionInformation gameSelectionConnInfo = serviceRegistryClient.getServiceConnectionInformation(GameSelectionMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessage(gameSelectionConnInfo, message);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException(String.format("Unable to send [%s] request to GameSelection service", message.getTypeName()), e);
		}
	}

}
