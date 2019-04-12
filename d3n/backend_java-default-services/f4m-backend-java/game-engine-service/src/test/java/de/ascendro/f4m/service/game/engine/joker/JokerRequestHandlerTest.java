package de.ascendro.f4m.service.game.engine.joker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.game.engine.GameEngineMessageTypes;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.joker.PurchaseFiftyFiftyRequest;
import de.ascendro.f4m.service.game.engine.model.joker.PurchaseFiftyFiftyResponse;
import de.ascendro.f4m.service.game.engine.server.GameEngine;
import de.ascendro.f4m.service.game.engine.server.MessageCoordinator;
import de.ascendro.f4m.service.game.selection.model.game.JokerType;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.ServiceUtil;

@RunWith(MockitoJUnitRunner.class)
public class JokerRequestHandlerTest {

	private static final String USER_ID = "user-id";

	private static final String GAME_INSTANCE_ID = "game-inst-id";

	@Mock
	private GameEngine gameEngine;
	
	@Mock
	private MessageCoordinator serviceCommunicator;
	
	@Mock
	private ServiceUtil serviceUtil;
	
	@Mock
	private SessionWrapper sessionWrapper;
	
	@InjectMocks
	private JokerRequestHandler service;
	
	private JsonUtil jsonUtil = new JsonUtil();

	@Test
	public void testOnPurchaseJokerWithPayment() {
		PurchaseFiftyFiftyRequest jokerRequest = new PurchaseFiftyFiftyRequest();
		jokerRequest.setGameInstanceId(GAME_INSTANCE_ID);
		JsonMessage<PurchaseFiftyFiftyRequest> message = new JsonMessage<>(jsonUtil.toJson(jokerRequest));
		message.setContent(jokerRequest);
		message.setClientInfo(new ClientInfo(USER_ID));
		SessionWrapper sessionWrapper = mock(SessionWrapper.class);
		when(gameEngine.initiateJokerPurchase(any(JokerType.class), eq(sessionWrapper), eq(message))).thenReturn(false);
		
		JsonMessageContent response = service.onPurchaseJoker(message, sessionWrapper);
		
		assertNull(response);
		verify(gameEngine).initiateJokerPurchase(any(JokerType.class), eq(sessionWrapper), eq(message));
	}

	@Test
	public void testOnPurchaseJokerNoPayment() {
		PurchaseFiftyFiftyRequest jokerRequest = new PurchaseFiftyFiftyRequest();
		GameInstance gameInstance = mock(GameInstance.class);
		jokerRequest.setGameInstanceId(GAME_INSTANCE_ID);
		JsonMessage<PurchaseFiftyFiftyRequest> message = new JsonMessage<>(jsonUtil.toJson(jokerRequest));
		message.setContent(jokerRequest);
		SessionWrapper sessionWrapper = mock(SessionWrapper.class);
		when(gameEngine.initiateJokerPurchase(any(JokerType.class), eq(sessionWrapper), eq(message))).thenReturn(true);
		when(gameEngine.finalizeJokerPurchase(any(JokerType.class), eq(GAME_INSTANCE_ID), eq(0))).thenReturn(gameInstance);
		String[] answers = new String[]{"bla", "blabla"};
		when(gameInstance.calculateQuestionRemovedAnswers(anyInt())).thenReturn(answers);
		
		JsonMessageContent response = service.onPurchaseJoker(message, sessionWrapper);
		
		assertNotNull(response);
		assertTrue(response instanceof PurchaseFiftyFiftyResponse);
		assertEquals(2, ((PurchaseFiftyFiftyResponse)response).getRemovedAnswers().length);
		assertEquals("bla", ((PurchaseFiftyFiftyResponse)response).getRemovedAnswers()[0]);
		assertEquals("blabla", ((PurchaseFiftyFiftyResponse)response).getRemovedAnswers()[1]);
		verify(gameEngine).initiateJokerPurchase(any(JokerType.class), eq(sessionWrapper), eq(message));	
		verify(gameEngine).finalizeJokerPurchase(any(JokerType.class), any(String.class), anyInt());	
	}

	@Test
	public void testHandlePurchaseJokerRequest() {
		PurchaseFiftyFiftyRequest jokerRequest = new PurchaseFiftyFiftyRequest();
		jokerRequest.setGameInstanceId(GAME_INSTANCE_ID);
		GameInstance gameInstance = mock(GameInstance.class);
		String[] answers = new String[]{"bla", "blabla"};
		when(gameInstance.calculateQuestionRemovedAnswers(anyInt())).thenReturn(answers);
		when(gameEngine.finalizeJokerPurchase(any(JokerType.class), eq(GAME_INSTANCE_ID), eq(0))).thenReturn(gameInstance);
		
		JsonMessage<PurchaseFiftyFiftyRequest> message = new JsonMessage<>(GameEngineMessageTypes.PURCHASE_FIFTY_FIFTY);
		message.setContent(jokerRequest);
		JsonMessageContent response = service.handlePurchaseJokerRequest(message, sessionWrapper, 0);
		
		assertNotNull(response);
		assertTrue(response instanceof PurchaseFiftyFiftyResponse);
		assertEquals(2, ((PurchaseFiftyFiftyResponse)response).getRemovedAnswers().length);
		assertEquals("bla", ((PurchaseFiftyFiftyResponse)response).getRemovedAnswers()[0]);
		assertEquals("blabla", ((PurchaseFiftyFiftyResponse)response).getRemovedAnswers()[1]);
		verify(gameEngine).finalizeJokerPurchase(any(JokerType.class), any(String.class), anyInt());	
	}

}
