package de.ascendro.f4m.server.request.jackpot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import de.ascendro.f4m.server.game.GameUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.multiplayer.Invitation;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.session.SessionWrapper;

public class JackpotDataGetter {

	private final JsonUtil jsonUtil;
	private final PaymentServiceCommunicator paymentServiceCommunicator;

	@Inject
	public JackpotDataGetter(JsonUtil jsonUtil,
			PaymentServiceCommunicator dependencyServicesCommunicator) {
		this.jsonUtil = jsonUtil;
		this.paymentServiceCommunicator = dependencyServicesCommunicator;
	}

	public boolean synchronizeUpdatedData(JsonArray gameList, JsonMessage<?> sourceMessage,
			SessionWrapper sessionWrapper, JsonMessageContent response) {
		
		String tenantId = sourceMessage.getTenantId();
		boolean sendResponseNow = true;
		AtomicInteger numberOfResponsesToWait = new AtomicInteger();
		
		List<PaymentGetJackpotRequestInfo> getJackpotGamesList = new ArrayList<>();
		
		for (JsonElement gameElement : gameList) {
			Game game = jsonUtil.fromJson(gameElement, Game.class);
			if (game.getMultiplayerGameInstanceId() != null && !GameUtil.isFreeGame(game, null)) {
				PaymentGetJackpotRequestInfo requestInfo = new PaymentGetJackpotRequestInfo(game, sourceMessage,
						sessionWrapper);
				numberOfResponsesToWait.incrementAndGet();
				fillRequestInfo(requestInfo, numberOfResponsesToWait, response);
				sendResponseNow = false;
				getJackpotGamesList.add(requestInfo);
			}
		}

		for (PaymentGetJackpotRequestInfo item: getJackpotGamesList){
			paymentServiceCommunicator.sendGetJackpotRequest(item.getGame().getMultiplayerGameInstanceId(),
			tenantId, item);
		}
		
		return sendResponseNow;
	}

	public boolean synchronizeUpdatedDataInvitation(List<Invitation> invitationList, JsonMessage<?> sourceMessage,
			SessionWrapper sessionWrapper, JsonMessageContent response) {
		
		boolean sendResponseNow = true;
		String tenantId = sourceMessage.getTenantId();
		AtomicInteger numberOfResponsesToWait = new AtomicInteger();
		JsonArray publicGames = (JsonArray) jsonUtil.toJsonElement(invitationList);
		List<PaymentGetJackpotRequestInfo> getJackpotGamesList = new ArrayList<>();
		
		for (JsonElement gameElement : publicGames) {
			Invitation invitation = jsonUtil.fromJson(gameElement, Invitation.class);
			if (invitation.getMultiplayerGameInstanceId() != null && !invitation.isGameIsFree() ) {
				PaymentGetJackpotRequestInfo requestInfo = new PaymentGetJackpotRequestInfo(invitation, sourceMessage,
						sessionWrapper);
				numberOfResponsesToWait.incrementAndGet();
				fillRequestInfo(requestInfo, numberOfResponsesToWait, response);
				sendResponseNow = false;
				
				getJackpotGamesList.add(requestInfo);
			}
		}
		
		for (PaymentGetJackpotRequestInfo item: getJackpotGamesList){
			paymentServiceCommunicator.sendGetJackpotRequest(item.getInvitation().getMultiplayerGameInstanceId(),
			tenantId, item);
		}
		
		return sendResponseNow;
	}
	
	public boolean synchronizeUpdatedDataHistory(List<SimpleGameInfo> gameHistoryList, JsonMessage<?> sourceMessage,
			SessionWrapper sessionWrapper, JsonMessageContent response) {
		boolean sendResponseNow = true;
		
		String tenantId = sourceMessage.getTenantId();
		AtomicInteger numberOfResponsesToWait = new AtomicInteger();
		List<PaymentGetJackpotRequestInfo> getJackpotGamesList = new ArrayList<>();
		
		for (SimpleGameInfo game : gameHistoryList) {
			if (game.getMultiplayerGameInstanceId() != null && !game.isGameFree()) {
				PaymentGetJackpotRequestInfo requestInfo = new PaymentGetJackpotRequestInfo(game, sourceMessage,
						sessionWrapper);
				numberOfResponsesToWait.incrementAndGet();
				fillRequestInfo(requestInfo, numberOfResponsesToWait, response);
				sendResponseNow = false;
				getJackpotGamesList.add(requestInfo);
			}
		}

		
		for (PaymentGetJackpotRequestInfo item: getJackpotGamesList){
			paymentServiceCommunicator.sendGetJackpotRequest(item.getGameInfo().getMultiplayerGameInstanceId(),
			tenantId, item);
		}
		
		return sendResponseNow;
	}

	private void fillRequestInfo(PaymentGetJackpotRequestInfo requestInfo, AtomicInteger numberOfResponsesToWait,
			JsonMessageContent response) {
		requestInfo.setNumberOfExpectedResponses(numberOfResponsesToWait);
		requestInfo.setResponseToForward(response);
	}

	

	public JsonArray modifyGameListWithJackpot(JsonArray gameList, Game gameToChange) {
		JsonArray resultArray = new JsonArray();
		for (JsonElement gameElement : gameList) {
			Game game = jsonUtil.fromJson(gameElement, Game.class);
			if (game.getGameId().equals(gameToChange.getGameId())) {
				resultArray.add(jsonUtil.toJsonElement(gameToChange));
			} else {
				resultArray.add(gameElement);
			}
		}
		return resultArray;
	}

	public List<Invitation> modifyInvitationGameListWithJackpot(List<Invitation> invitationList,
			Invitation invitationToChange) {
		List<Invitation> resultArray = new ArrayList<>();
		for (Invitation element : invitationList) {
			if (element.getMultiplayerGameInstanceId().equals(invitationToChange.getMultiplayerGameInstanceId())) {
				resultArray.add(invitationToChange);
			} else {
				resultArray.add(element);
			}
		}
		return resultArray;
	}
	
}
