package de.ascendro.f4m.server.request.jackpot;

import java.util.concurrent.atomic.AtomicInteger;

import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.multiplayer.Invitation;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.MessageSource;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.session.SessionWrapper;

public class PaymentGetJackpotRequestInfo extends RequestInfoImpl{

	private Game game;
	private AtomicInteger numberOfExpectedResponses;
	private JsonMessageContent responseToForward;
	private Invitation invitation;
	private SimpleGameInfo gameInfo;

	
	public PaymentGetJackpotRequestInfo(Game game, JsonMessage<?> sourceMessage, Object messageSource) {
		super(sourceMessage, (MessageSource) messageSource);
		this.game = game;
	}

	public PaymentGetJackpotRequestInfo(Invitation invitation, JsonMessage<?> sourceMessage, MessageSource messageSource) {
		super(sourceMessage, messageSource);
		this.invitation = invitation;
	}

	public PaymentGetJackpotRequestInfo(SimpleGameInfo gameInfo, JsonMessage<?> sourceMessage, MessageSource messageSource) {
		super(sourceMessage, messageSource);
		this.gameInfo = gameInfo;
	}
	
	
	public AtomicInteger getNumberOfExpectedResponses() {
		return numberOfExpectedResponses;
	}

	public void setNumberOfExpectedResponses(AtomicInteger numberOfExpectedResponses) {
		this.numberOfExpectedResponses = numberOfExpectedResponses;
	}
	
	
	public Game getGame() {
		return game;
	}

	public JsonMessageContent getResponseToForward() {
		return responseToForward;
	}

	public void setResponseToForward(JsonMessageContent responseToForward) {
		this.responseToForward = responseToForward;
	}

	public Invitation getInvitation() {
		return invitation;
	}

	public void setInvitation(Invitation invitation) {
		this.invitation = invitation;
	}

	public SimpleGameInfo getGameInfo() {
		return gameInfo;
	}

	public void setGameInfo(SimpleGameInfo gameInfo) {
		this.gameInfo = gameInfo;
	}

}
