package de.ascendro.f4m.service.game.engine;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.game.engine.model.advertisement.ShowAdvertisementRequest;
import de.ascendro.f4m.service.game.engine.model.answer.AnswerQuestionRequest;
import de.ascendro.f4m.service.game.engine.model.answer.AnswerQuestionResponse;
import de.ascendro.f4m.service.game.engine.model.answer.NextStepRequest;
import de.ascendro.f4m.service.game.engine.model.cancel.CancelGameRequest;
import de.ascendro.f4m.service.game.engine.model.end.EndGameRequest;
import de.ascendro.f4m.service.game.engine.model.joker.JokersAvailableRequest;
import de.ascendro.f4m.service.game.engine.model.joker.JokersAvailableResponse;
import de.ascendro.f4m.service.game.engine.model.joker.PurchaseFiftyFiftyRequest;
import de.ascendro.f4m.service.game.engine.model.joker.PurchaseFiftyFiftyResponse;
import de.ascendro.f4m.service.game.engine.model.joker.PurchaseHintRequest;
import de.ascendro.f4m.service.game.engine.model.joker.PurchaseHintResponse;
import de.ascendro.f4m.service.game.engine.model.joker.PurchaseImmediateAnswerRequest;
import de.ascendro.f4m.service.game.engine.model.joker.PurchaseSkipRequest;
import de.ascendro.f4m.service.game.engine.model.ready.ReadyToPlayRequest;
import de.ascendro.f4m.service.game.engine.model.register.RegisterRequest;
import de.ascendro.f4m.service.game.engine.model.register.RegisterResponse;
import de.ascendro.f4m.service.game.engine.model.start.game.StartGameRequest;
import de.ascendro.f4m.service.game.engine.model.start.game.StartGameResponse;
import de.ascendro.f4m.service.game.engine.model.start.step.StartStepRequest;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;

public class GameEngineMessageTypeMapper extends JsonMessageTypeMapImpl {
	private static final long serialVersionUID = -4575812985735172610L;

	public GameEngineMessageTypeMapper() {
		init();
	}

	protected void init() {
		register(GameEngineMessageTypes.REGISTER, new TypeToken<RegisterRequest>() {});
		register(GameEngineMessageTypes.REGISTER_RESPONSE, new TypeToken<RegisterResponse>() {});

		register(GameEngineMessageTypes.START_GAME, new TypeToken<StartGameRequest>() {});
		register(GameEngineMessageTypes.START_GAME_RESPONSE, new TypeToken<StartGameResponse>() {});

		register(GameEngineMessageTypes.READY_TO_PLAY, new TypeToken<ReadyToPlayRequest>() {});
		register(GameEngineMessageTypes.READY_TO_PLAY_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});

		register(GameEngineMessageTypes.START_STEP, new TypeToken<StartStepRequest>() {});

		register(GameEngineMessageTypes.ANSWER_QUESTION, new TypeToken<AnswerQuestionRequest>() {});
		register(GameEngineMessageTypes.ANSWER_QUESTION_RESPONSE, new TypeToken<AnswerQuestionResponse>() {});

		register(GameEngineMessageTypes.NEXT_STEP, new TypeToken<NextStepRequest>() {});

		register(GameEngineMessageTypes.END_GAME, new TypeToken<EndGameRequest>() {});

		register(GameEngineMessageTypes.HEALTH_CHECK, new TypeToken<EmptyJsonMessageContent>() {});
		register(GameEngineMessageTypes.HEALTH_CHECK_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});

		register(GameEngineMessageTypes.CANCEL_GAME, new TypeToken<CancelGameRequest>() {});
		register(GameEngineMessageTypes.CANCEL_GAME_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});

		register(GameEngineMessageTypes.SHOW_ADVERTISEMENT, new TypeToken<ShowAdvertisementRequest>() {});

		register(GameEngineMessageTypes.JOKERS_AVAILABLE, new TypeToken<JokersAvailableRequest>() {});
		register(GameEngineMessageTypes.JOKERS_AVAILABLE_RESPONSE, new TypeToken<JokersAvailableResponse>() {});

		register(GameEngineMessageTypes.PURCHASE_HINT, new TypeToken<PurchaseHintRequest>() {});
		register(GameEngineMessageTypes.PURCHASE_HINT_RESPONSE, new TypeToken<PurchaseHintResponse>() {});

		register(GameEngineMessageTypes.PURCHASE_SKIP, new TypeToken<PurchaseSkipRequest>() {});
		register(GameEngineMessageTypes.PURCHASE_SKIP_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});

		register(GameEngineMessageTypes.PURCHASE_FIFTY_FIFTY, new TypeToken<PurchaseFiftyFiftyRequest>() {});
		register(GameEngineMessageTypes.PURCHASE_FIFTY_FIFTY_RESPONSE, new TypeToken<PurchaseFiftyFiftyResponse>() {});

		register(GameEngineMessageTypes.PURCHASE_IMMEDIATE_ANSWER, new TypeToken<PurchaseImmediateAnswerRequest>() {});
		register(GameEngineMessageTypes.PURCHASE_IMMEDIATE_ANSWER_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});
	}
}
