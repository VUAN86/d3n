package de.ascendro.f4m.service.result.engine;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;
import de.ascendro.f4m.service.result.engine.model.calculate.CalculateMultiplayerResultsRequest;
import de.ascendro.f4m.service.result.engine.model.calculate.CalculateResultsRequest;
import de.ascendro.f4m.service.result.engine.model.calculate.CalculateResultsResponse;
import de.ascendro.f4m.service.result.engine.model.get.*;
import de.ascendro.f4m.service.result.engine.model.move.MoveResultsRequest;
import de.ascendro.f4m.service.result.engine.model.multiplayerGameResult.MultiplayerResultsBuddiesListRequest;
import de.ascendro.f4m.service.result.engine.model.multiplayerGameResult.MultiplayerResultsBuddiesListResponse;
import de.ascendro.f4m.service.result.engine.model.multiplayerGameResult.MultiplayerResultsGlobalListRequest;
import de.ascendro.f4m.service.result.engine.model.multiplayerGameResult.MultiplayerResultsGlobalListResponse;
import de.ascendro.f4m.service.result.engine.model.multiplayerGameResult.ResyncMultiplayerResultsRequest;
import de.ascendro.f4m.service.result.engine.model.respond.RespondToUserInteractionRequest;
import de.ascendro.f4m.service.result.engine.model.store.StoreUserWinningComponentRequest;

public class ResultEngineMessageTypeMapper extends JsonMessageTypeMapImpl {

	private static final long serialVersionUID = 8475051282009551795L;

	public ResultEngineMessageTypeMapper() {
		init();
	}

	protected void init() {

		// Calculate results for gameInstanceId
		register(ResultEngineMessageTypes.CALCULATE_RESULTS.getMessageName(), new TypeToken<CalculateResultsRequest>() {}.getType());
		register(ResultEngineMessageTypes.CALCULATE_RESULTS_RESPONSE.getMessageName(), new TypeToken<CalculateResultsResponse>() {}.getType());


		// Get Game statistic
		register(ResultEngineMessageTypes.GET_STATISTIC.getMessageName(), new TypeToken<GetStatisticRequest>() {}.getType());
		register(ResultEngineMessageTypes.GET_STATISTIC_RESPONSE.getMessageName(), new TypeToken<GetStatisticResponse>() {}.getType());

		// Get results by gameInstanceId
		register(ResultEngineMessageTypes.GET_RESULTS.getMessageName(), new TypeToken<GetResultsRequest>() {}.getType());
		register(ResultEngineMessageTypes.GET_RESULTS_RESPONSE.getMessageName(), new TypeToken<GetResultsResponse>() {}.getType());

		// Get results by gameInstanceId (internal)
		register(ResultEngineMessageTypes.GET_RESULTS_INTERNAL.getMessageName(), new TypeToken<GetResultsRequest>() {}.getType());
		register(ResultEngineMessageTypes.GET_RESULTS_INTERNAL_RESPONSE.getMessageName(), new TypeToken<GetResultsResponse>() {}.getType());

		// Move results from one user to another
		register(ResultEngineMessageTypes.MOVE_RESULTS.getMessageName(), new TypeToken<MoveResultsRequest>() {}.getType());
		register(ResultEngineMessageTypes.MOVE_RESULTS_RESPONSE.getMessageName(), new TypeToken<EmptyJsonMessageContent>() {}.getType());

		// Calculate results for finished game
		register(ResultEngineMessageTypes.CALCULATE_MULTIPLAYER_RESULTS.getMessageName(), new TypeToken<CalculateMultiplayerResultsRequest>() {}.getType());
		register(ResultEngineMessageTypes.CALCULATE_MULTIPLAYER_RESULTS_RESPONSE.getMessageName(), new TypeToken<EmptyJsonMessageContent>() {}.getType());

		// Get game results by duelId / tournamentId
		register(ResultEngineMessageTypes.GET_MULTIPLAYER_RESULTS.getMessageName(), new TypeToken<GetMultiplayerResultsRequest>() {}.getType());
		register(ResultEngineMessageTypes.GET_MULTIPLAYER_RESULTS_RESPONSE.getMessageName(), new TypeToken<GetMultiplayerResultsResponse>() {}.getType());

		// Respond to user interaction
		register(ResultEngineMessageTypes.RESPOND_TO_USER_INTERACTION.getMessageName(), new TypeToken<RespondToUserInteractionRequest>() {}.getType());
		register(ResultEngineMessageTypes.RESPOND_TO_USER_INTERACTION_RESPONSE.getMessageName(), new TypeToken<EmptyJsonMessageContent>() {}.getType());

		// Store user winning component
		register(ResultEngineMessageTypes.STORE_USER_WINNING_COMPONENT.getMessageName(), new TypeToken<StoreUserWinningComponentRequest>() {}.getType());
		register(ResultEngineMessageTypes.STORE_USER_WINNING_COMPONENT_RESPONSE.getMessageName(), new TypeToken<EmptyJsonMessageContent>() {}.getType());

		// List of completed games
		register(ResultEngineMessageTypes.COMPLETED_GAME_LIST.getMessageName(), new TypeToken<CompletedGameListRequest>() {}.getType());
		register(ResultEngineMessageTypes.COMPLETED_GAME_LIST_RESPONSE.getMessageName(), new TypeToken<CompletedGameListResponse>() {}.getType());

		register(ResultEngineMessageTypes.COMPLETED_GAME_LIST_FOR_USER.getMessageName(), new TypeToken<CompletedGameListForUserRequest>() {}.getType());
		register(ResultEngineMessageTypes.COMPLETED_GAME_LIST_FOR_USER_RESPONSE.getMessageName(), new TypeToken<CompletedGameListResponse>() {}.getType());

		// Multiplayer game results
		register(ResultEngineMessageTypes.MULTIPLAYER_RESULTS_GLOBAL_LIST.getMessageName(), new TypeToken<MultiplayerResultsGlobalListRequest>() {}.getType());
		register(ResultEngineMessageTypes.MULTIPLAYER_RESULTS_GLOBAL_LIST_RESPONSE.getMessageName(), new TypeToken<MultiplayerResultsGlobalListResponse>() {}.getType());

		register(ResultEngineMessageTypes.MULTIPLAYER_RESULTS_BUDDIES_LIST.getMessageName(), new TypeToken<MultiplayerResultsBuddiesListRequest>() {}.getType());
		register(ResultEngineMessageTypes.MULTIPLAYER_RESULTS_BUDDIES_LIST_RESPONSE.getMessageName(), new TypeToken<MultiplayerResultsBuddiesListResponse>() {}.getType());

		// Resync multiplayer results to elastic
		register(ResultEngineMessageTypes.RESYNC_MULTIPLAYER_RESULTS.getMessageName(), new TypeToken<ResyncMultiplayerResultsRequest>() {}.getType());
		register(ResultEngineMessageTypes.RESYNC_MULTIPLAYER_RESULTS_RESPONSE.getMessageName(), new TypeToken<EmptyJsonMessageContent>() {}.getType());
	}
}
