package de.ascendro.f4m.service.game.engine.joker;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.joker.PurchaseFiftyFiftyResponse;
import de.ascendro.f4m.service.game.engine.model.joker.PurchaseHintResponse;
import de.ascendro.f4m.service.game.engine.model.joker.PurchaseJokerRequest;
import de.ascendro.f4m.service.game.engine.server.GameEngine;
import de.ascendro.f4m.service.game.engine.server.MessageCoordinator;
import de.ascendro.f4m.service.game.selection.model.game.JokerType;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.ServiceUtil;

public class JokerRequestHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(JokerRequestHandler.class);

	private final GameEngine gameEngine;
	private final MessageCoordinator serviceCommunicator;
	private final ServiceUtil serviceUtil;

	@Inject
	public JokerRequestHandler(GameEngine gameEngine, MessageCoordinator serviceCommunicator, ServiceUtil serviceUtil) {
		super();
		this.gameEngine = gameEngine;
		this.serviceCommunicator = serviceCommunicator;
		this.serviceUtil = serviceUtil;
	}

	public JsonMessageContent onPurchaseJoker(JsonMessage<? extends PurchaseJokerRequest> message, SessionWrapper sessionWrapper) {
		final JokerType type = message.getContent().getType();
		boolean completedJokerPurchase = gameEngine.initiateJokerPurchase(type, sessionWrapper, message);
		if (completedJokerPurchase) {
			return handlePurchaseJokerRequest(message, sessionWrapper, serviceUtil.getMessageTimestamp());
		} else {
			LOGGER.debug("response delayed due to joker payment of type {} for user {} in gameInstance {}", type,
					message.getClientInfo().getUserId(), message.getContent().getGameInstanceId());
			return null;
		}
	}

	public JsonMessageContent handlePurchaseJokerRequest(JsonMessage<? extends PurchaseJokerRequest> message, Object sessionWrapper, long receiveTimestamp) {
		final PurchaseJokerRequest jokerRequest = message.getContent();
		final String gameInstanceId = jokerRequest.getGameInstanceId();
		final JokerType type = jokerRequest.getType();
		final int questionIndex = jokerRequest.getQuestion();
		GameInstance gameInstance = gameEngine.finalizeJokerPurchase(type, gameInstanceId, questionIndex);
		switch (type) {
		case SKIP:
			questionSkipped(gameInstance, (SessionWrapper) sessionWrapper, message.getClientInfo(), questionIndex, receiveTimestamp);
			return new EmptyJsonMessageContent();
		case HINT:
			return new PurchaseHintResponse(gameInstance.getQuestionHint(questionIndex));
		case FIFTY_FIFTY:
			return new PurchaseFiftyFiftyResponse(gameInstance.calculateQuestionRemovedAnswers(questionIndex));
		case IMMEDIATE_ANSWER:
			return new EmptyJsonMessageContent();
		default:
			throw new IllegalArgumentException("Unknown joker type: " + type);
		}
	}

	private void questionSkipped(GameInstance gameInstance, SessionWrapper sessionWrapper, ClientInfo clientInfo, int questionIndex, long receiveTimestamp) {
		if (questionIndex >= gameInstance.getNumberOfQuestionsIncludingSkipped()) {
			throw new F4MValidationFailedException("Question attempted to skip is out of expected bounds: " + gameInstance.getNumberOfQuestionsIncludingSkipped());
		} else if (gameInstance.hasAnyQuestion()) {
			serviceCommunicator.performNextGameStepOrQuestion(sessionWrapper, clientInfo.getClientId(), gameInstance.getId(), receiveTimestamp, true);
		} else if (! gameInstance.getGameState().isCompleted()) {
			serviceCommunicator.performGameEnd(sessionWrapper, gameInstance, clientInfo);
		}
	}

}
