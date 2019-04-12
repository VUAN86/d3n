package de.ascendro.f4m.service.game.engine.server;

import static org.hamcrest.collection.IsArrayContainingInAnyOrder.arrayContainingInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.service.game.engine.health.HealthCheckManager;
import de.ascendro.f4m.service.game.engine.joker.JokerRequestHandler;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.Question;
import de.ascendro.f4m.service.game.engine.model.answer.AnswerQuestionResponse;
import de.ascendro.f4m.service.game.engine.server.subscription.EventSubscriptionManager;
import de.ascendro.f4m.service.game.usergameaccess.UserGameAccessService;
import de.ascendro.f4m.service.util.ServiceUtil;

public class GameEngineServiceServerMessageHandlerTest {

	private GameEngineServiceServerMessageHandler messageHandler;

	@Mock
	private GameEngine gameEngine;
	@Mock
	private JokerRequestHandler jokerRequestHandler;
	@Mock
	private HealthCheckManager healthCheckManager;
	@Mock
	private ServiceUtil serviceUtil;
	@Mock
	private EventSubscriptionManager eventSubscriptionManager;
	@Mock
	private MessageCoordinator messageCoordinator;

	@Mock
	private GameInstance gameInstance;
	
	@Mock
	private CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao;
	
	private UserGameAccessService gameChecker;

	private List<Question> questions;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		messageHandler = new GameEngineServiceServerMessageHandler(gameEngine, jokerRequestHandler, healthCheckManager,
				serviceUtil, commonMultiplayerGameInstanceDao, eventSubscriptionManager,
				messageCoordinator, gameChecker);

		questions = getQuestions();
		IntStream.range(0, questions.size())
				.forEach(i -> when(gameInstance.getQuestion(i)).thenReturn(questions.get(i)));
	}

	private List<Question> getQuestions() {
		Question q1 = new Question();
		q1.setCorrectAnswers("a", "c");

		Question q2 = new Question();
		q2.setCorrectAnswers("2");

		return Arrays.asList(q1, q2);
	}

	@Test
	public void testGetAnswerQuestionResponseWithAnswers() {
		when(gameInstance.instantlySendAnswer()).thenReturn(true);

		for (int i = 0; i < questions.size(); i++) {
			AnswerQuestionResponse response = messageHandler.getAnswerQuestionResponse(gameInstance, i, false);
			String[] correctAnswers = questions.get(i).getCorrectAnswers();
			assertThat(correctAnswers, arrayContainingInAnyOrder(response.getCorrectAnswers()));
		}
	}

	@Test
	public void testGetAnswerQuestionResponseWithoutAnswers() {
		when(gameInstance.instantlySendAnswer()).thenReturn(false);

		for (int i = 0; i < questions.size(); i++) {
			AnswerQuestionResponse response = messageHandler.getAnswerQuestionResponse(gameInstance, i, false);
			assertEquals(0, response.getCorrectAnswers().length);
		}
	}

}
