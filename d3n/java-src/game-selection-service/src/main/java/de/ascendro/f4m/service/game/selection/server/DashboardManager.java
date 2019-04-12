package de.ascendro.f4m.service.game.selection.server;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.dashboard.dao.DashboardDao;
import de.ascendro.f4m.server.dashboard.move.dao.MoveDashboardDao;
import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.game.selection.client.communicator.ResultEngineCommunicator;
import de.ascendro.f4m.service.game.selection.model.dashboard.ApiDuel;
import de.ascendro.f4m.service.game.selection.model.dashboard.ApiQuiz;
import de.ascendro.f4m.service.game.selection.model.dashboard.ApiTournament;
import de.ascendro.f4m.service.game.selection.model.dashboard.ApiUserTournament;
import de.ascendro.f4m.service.game.selection.model.dashboard.GetDashboardRequest;
import de.ascendro.f4m.service.game.selection.model.dashboard.GetDashboardResponse;
import de.ascendro.f4m.service.game.selection.model.dashboard.MostPlayedGame;
import de.ascendro.f4m.service.game.selection.model.dashboard.NextInvitation;
import de.ascendro.f4m.service.game.selection.model.dashboard.PlayedDuelInfo;
import de.ascendro.f4m.service.game.selection.model.dashboard.PlayedGameInfo;
import de.ascendro.f4m.service.game.selection.model.dashboard.PlayedTournamentInfo;
import de.ascendro.f4m.service.game.selection.model.dashboard.SpecialGameInfo;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CreatedBy;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.Invitation;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerUserGameInstance;
import de.ascendro.f4m.service.game.selection.model.multiplayer.PublicGameFilter;
import de.ascendro.f4m.service.game.selection.model.multiplayer.PublicGameFilterBuilder;
import de.ascendro.f4m.service.game.usergameaccess.UserGameAccessService;
import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.profile.model.api.ApiProfileBasicInfo;
import de.ascendro.f4m.service.session.SessionWrapper;

public class DashboardManager {
	
	private static final int REVERSED_ORDER = -1;

	private static final Logger LOGGER = LoggerFactory.getLogger(DashboardManager.class);

	private final DashboardDao dashboardDao;
	private final GameSelector gameSelector;
	private final MultiplayerGameInstanceManager mgiManager;
	private final CommonProfileAerospikeDao profileDao;
	private final MoveDashboardDao moveDashboardDao;
	private final ResultEngineCommunicator resultEngineCommunicator;
	private final UserGameAccessService userGameAccessService;

	@Inject
	public DashboardManager(DashboardDao dashboardDao, GameSelector gameSelector,
			MultiplayerGameInstanceManager mgiManager, CommonProfileAerospikeDao profileDao,
			MoveDashboardDao moveDashboardDao, ResultEngineCommunicator resultEngineCommunicator,
			UserGameAccessService userGameAccessService) {
		this.dashboardDao = dashboardDao;
		this.gameSelector = gameSelector;
		this.mgiManager = mgiManager;
		this.profileDao = profileDao;
		this.moveDashboardDao = moveDashboardDao;
		this.resultEngineCommunicator = resultEngineCommunicator;
		this.userGameAccessService = userGameAccessService;
	}
	
	public GetDashboardResponse getDashboard(ClientInfo clientInfo, GetDashboardRequest request) {
		GetDashboardResponse dashboard = new GetDashboardResponse();
		if (isTrue(request.getQuiz())) {
			addGameTypeInfo(GameType.QUIZ24, clientInfo, dashboard, this::addQuizInfo);
			dashboard.setQuiz(collectGameTypeInfo(GameType.QUIZ24, clientInfo, this::collectApiQuizInfo));
		}
		if (isTrue(request.getDuel())) {
			dashboard.setDuel(collectGameTypeInfo(GameType.DUEL, clientInfo, this::collectApiDuelInfo));
		}
		if (isTrue(request.getUserTournament())) {
			dashboard.setUserTournament(
					collectGameTypeInfo(GameType.USER_TOURNAMENT, clientInfo, this::collectApiUserTournamentInfo));
		}
		if (isTrue(request.getUserLiveTournament())) {
			dashboard.setUserLiveTournament(
					collectGameTypeInfo(GameType.USER_LIVE_TOURNAMENT, clientInfo, this::collectApiUserTournamentInfo));
		}
		if (isTrue(request.getTournament())) {
			dashboard.setTournament(
					collectGameTypeInfo(GameType.TOURNAMENT, clientInfo, this::collectApiTournamentInfo));
		}
		if (isTrue(request.getLiveTournament())) {
			dashboard.setLiveTournament(
					collectGameTypeInfo(GameType.LIVE_TOURNAMENT, clientInfo, this::collectApiTournamentInfo));
		}
		return dashboard;
	}

	private void addGameTypeInfo(GameType type, ClientInfo clientInfo, GetDashboardResponse dashboard,
			BiConsumer<ClientInfo, GetDashboardResponse> addInfoConsumer) {
		try {
			addInfoConsumer.accept(clientInfo, dashboard);
		} catch (Exception e) {
			LOGGER.error("Failed to prepare [{}] data", type.name(), e);
		}
	}

	private <T> T collectGameTypeInfo(GameType type, ClientInfo clientInfo,
			BiFunction<ClientInfo, GameType, T> addInfoConsumer) {
		try {
			return addInfoConsumer.apply(clientInfo, type);
		} catch (Exception e) {
			LOGGER.error("Failed to prepare [{}] data", type.name(), e);
			return null;
		}
	}

	public GetDashboardResponse addLastUserTournamentPlacement(GetDashboardResponse dashboard,
			JsonMessage<GetDashboardRequest> message, SessionWrapper sessionWrapper) {
		GetDashboardResponse dashboardResponse = dashboard;
		try {
			ApiUserTournament userTournament = dashboard.getUserTournament();
			if (userTournament != null && !hasPlacement(userTournament)) {
				PlayedTournamentInfo lastTournamentInfo = userTournament.getLastTournament().getTournamentInfo();
				if (lastTournamentInfo != null) {
					resultEngineCommunicator.requestGetMultiplayerResults(message, sessionWrapper, dashboard,
							lastTournamentInfo.getGameInstanceId());
					dashboardResponse = null;
				}
			}
		} catch (Exception e) {
			LOGGER.error("Failed to add last user tournament placement for user [{}]", message.getUserId(), e);
		}
		return dashboardResponse;
	}
	
	private boolean hasPlacement(ApiUserTournament userTournament) {
		return userTournament.getLastTournament() != null && userTournament.getLastTournament().tournamentHasPlacement();
	}
	
	private GetDashboardResponse addQuizInfo(ClientInfo clientInfo, GetDashboardResponse dashboard) {
		ApiQuiz quiz = collectApiQuizInfo(clientInfo, GameType.QUIZ24);
		
		dashboard.setQuiz(quiz);
		return dashboard;
	}

	private ApiQuiz collectApiQuizInfo(ClientInfo clientInfo, GameType gameType) {
		List<Game> quizGames = gameSelector.getGameListByType(clientInfo.getTenantId(), clientInfo.getAppId() ,gameType);
		List<String> gameIds = quizGames.stream()
				.filter(quiz -> !quiz.isSpecial())
				.map(Game::getGameId)
				.collect(Collectors.toList());
		PlayedGameInfo lastPlayedGame = dashboardDao.getLastPlayedGame(clientInfo.getTenantId(), clientInfo.getUserId(), gameType);
		MostPlayedGame mostPlayedGame = dashboardDao.getMostPlayedGame(gameIds);
		
		List<Game> specialGames = quizGames.stream()
				.filter(game -> game.isSpecial())
				.collect(Collectors.toList());
		int numberOfSpecialGamesAvailable = specialGames.size();
		
		Optional<Game> lastSpecialGame = specialGames.stream()
			.sorted((o1, o2) -> REVERSED_ORDER * o1.getGameId().compareTo(o2.getGameId()))
			.filter(game -> userGameAccessService.isVisible(game, clientInfo.getUserId()))
			.findFirst();
		
		ApiQuiz quiz = new ApiQuiz();
		quiz.setNumberOfGamesAvailable(gameIds.size()); // all games that are not special games
		quiz.setLastPlayedGame(lastPlayedGame);
		quiz.setMostPlayedGame(mostPlayedGame);
		quiz.setNumberOfSpecialGamesAvailable(numberOfSpecialGamesAvailable);
		quiz.setLatestSpecialGame(lastSpecialGame.isPresent() ? getSpecialGameInfo(lastSpecialGame.get(), clientInfo.getUserId()) : null);
		return quiz;
	}
	
    private SpecialGameInfo getSpecialGameInfo(Game game, String userId) {
		SpecialGameInfo gameInfo = new SpecialGameInfo();
		gameInfo.setGameId(game.getGameId());
		gameInfo.setTitle(game.getTitle());
		gameInfo.setDateTime(game.getStartDateTime());
		gameInfo.setBannerMediaId(game.getBannerMediaId());
		gameInfo.setEntryFeeBatchSize(game.getEntryFeeBatchSize());
		Long userGameCountLeft = Long.valueOf(userGameAccessService.getRemainingPlayTimes(game, userId));
		gameInfo.setUserRemainingPlayCount(userGameCountLeft);
		gameInfo.setEntryFeeAmount(game.getEntryFeeAmount());
		gameInfo.setEntryFeeCurrency(game.getEntryFeeCurrency().name());
		return gameInfo;
	}

	private ApiDuel collectApiDuelInfo(ClientInfo clientInfo, GameType gameType) {
		ApiDuel duel = new ApiDuel();
		duel.setNextInvitation(getNextInvitationByGameType(clientInfo, gameType));
        duel.setNextPublicGame(getNextPublicGame(clientInfo, gameType));
        duel.setNumberOfInvitations(getActiveInvitationsByGameType(clientInfo, gameType).size());
        duel.setNumberOfPublicGames(getNumberOfActivePublicGames(clientInfo, gameType));
        
        duel.setLastDuel(getLastPlayedGameByGameType(clientInfo, gameType));
		return duel;
	}
    
	private ApiUserTournament collectApiUserTournamentInfo(ClientInfo clientInfo, GameType gameType) {
		ApiUserTournament userTournament = new ApiUserTournament();
		addUserTournamentBaseData(clientInfo, userTournament, gameType);
		userTournament.setLastTournament(getLastPlayedGameByGameType(clientInfo, gameType));
		return userTournament;
	}
    
	private ApiTournament collectApiTournamentInfo(ClientInfo clientInfo, GameType gameType) {
		ApiTournament tournament = new ApiTournament();
    	addTournamentBaseData(clientInfo, tournament, gameType);
		return tournament;
	}

	private void addTournamentBaseData(ClientInfo clientInfo, ApiTournament tournament, GameType gameType) {
		tournament.setNextInvitation(getNextInvitationByGameType(clientInfo, gameType));
    	tournament.setNextTournament(getNextPublicGame(clientInfo, gameType));
    	tournament.setNumberOfInvitations(getActiveInvitationsByGameType(clientInfo, gameType).size());
    	tournament.setNumberOfTournaments(getNumberOfActivePublicGames(clientInfo, gameType));
	}

	private void addUserTournamentBaseData(ClientInfo clientInfo, ApiTournament tournament, GameType gameType) {
		tournament.setNextInvitation(getNextInvitationByGameType(clientInfo, gameType));
		tournament.setNextTournament(new NextInvitation());
		tournament.setNumberOfInvitations(getActiveInvitationsByGameType(clientInfo, gameType).size());
		final long numberOfTournaments = gameSelector.getNumberOfGamesByType(clientInfo.getTenantId(),
				clientInfo.getAppId(), gameType);
		tournament.setNumberOfTournaments(numberOfTournaments);
	}
	
	private NextInvitation getNextInvitationByGameType(ClientInfo clientInfo, GameType type) {
		Invitation invitation = mgiManager.getInvitation(clientInfo.getTenantId(), clientInfo.getAppId(), clientInfo.getUserId(), type);
		return getNextInvitation(invitation);
	}
	
	private PlayedGameInfo getLastPlayedGameByGameType(ClientInfo clientInfo, GameType type) {
		PlayedGameInfo playedGameInfo = dashboardDao.getLastPlayedGame(clientInfo.getTenantId(), clientInfo.getUserId(), type);
		if (type == GameType.DUEL) {
			addOpponentInfo(playedGameInfo);
		}
		return playedGameInfo;
	}
	
	private List<Invitation> getActiveInvitationsByGameType(ClientInfo clientInfo, GameType type) {
		FilterCriteria filterCriteria = new FilterCriteria();
		filterCriteria.setLimit(Integer.MAX_VALUE);
		filterCriteria.addSearchBy(CustomGameConfig.PROPERTY_GAME_TYPE, type.name());
		return mgiManager.getInvitationList(clientInfo.getTenantId(), clientInfo.getAppId(),
				clientInfo.getUserId(), Arrays.asList(MultiplayerGameInstanceState.INVITED.name()), CreatedBy.OTHERS, filterCriteria);
	}
	
	private NextInvitation getNextPublicGame(ClientInfo clientInfo, GameType type) {
		PublicGameFilter publicGameFilter = PublicGameFilterBuilder.create(clientInfo.getAppId())
				.withGameType(type)
				.withNotByUserId(clientInfo.getUserId())
				.build();
		Invitation nextPublicGame = mgiManager.getNextPublicGame(publicGameFilter);
		return getNextInvitation(nextPublicGame);
	}
	
	private long getNumberOfActivePublicGames(ClientInfo clientInfo, GameType type) {
		PublicGameFilter publicGameFilter = PublicGameFilterBuilder.create(clientInfo.getAppId())
				.withGameType(type)
				.withNotByUserId(clientInfo.getUserId())
				.build();
		return mgiManager.getNumberOfPublicGames(publicGameFilter);
	}

	private NextInvitation getNextInvitation(Invitation invitation) {
		mgiManager.addGameInfo(Arrays.asList(invitation));
		mgiManager.addInvitationUserInfo(Arrays.asList(invitation), i -> i.getInviter().getUserId(), (i, p) -> i.setInviter(p));
		return new NextInvitation(invitation);
	}
	
	private void addOpponentInfo(PlayedGameInfo gameInfo) {
		PlayedDuelInfo duelInfo = gameInfo.getDuelInfo();
		if (duelInfo != null && duelInfo.getOpponentUserId() != null) {
			ApiProfileBasicInfo opponentInfo = profileDao.getProfileBasicInfo(duelInfo.getOpponentUserId());
			duelInfo.setOpponentInfo(opponentInfo);
		}
	}
	
	public void updatePlayedGame(String tenantId, String userId, PlayedGameInfo gameInfo) {
		if (isValidLastPlayedGame(userId, gameInfo)) {
			dashboardDao.updateLastPlayedGame(tenantId, userId, gameInfo);
		}
	}

	private boolean isValidLastPlayedGame(String userId, PlayedGameInfo gameInfo) {
		if (gameInfo.isRematch()) {
			return false;
		}
		if (gameInfo.getType().equals(GameType.DUEL) && !gameInfo.getDuelInfo().getGameCreatorId().equals(userId)) {
			return false;
		}
		return true;
	}

	public void updateFinishedMultiplayerGame(String tenantId, String mgiId) {
		CustomGameConfig multiplayerGameConfig = mgiManager.getMultiplayerGameConfig(mgiId);
		Game game = gameSelector.getGame(multiplayerGameConfig.getGameId());
		
		List<MultiplayerUserGameInstance> userGameInstances = mgiManager.getMultiplayerUserGameInstances(mgiId, MultiplayerGameInstanceState.CALCULATED);
		int numberOfParticipants = userGameInstances.size();
		userGameInstances.forEach(userGameInstance -> {
			PlayedGameInfo gameInfo = new PlayedGameInfo(game.getGameId(), game.getType(), game.getTitle(), multiplayerGameConfig.isRematch());
			if (game.isDuel()) {
				gameInfo.setDuelInfo(getFinishedDuelInfo(userGameInstance.getUserId(), userGameInstances, multiplayerGameConfig.getGameCreatorId()));
			} else if (game.isTournament()) {
				gameInfo.setTournamentInfo(getFinishedTournamentInfo(mgiId, userGameInstance, numberOfParticipants));
			}
			updatePlayedGame(tenantId, userGameInstance.getUserId(), gameInfo);
		});
	}
	
	private PlayedDuelInfo getFinishedDuelInfo(String playerId, List<MultiplayerUserGameInstance> userGameInstances, String gameCreatorId) {
		Optional<String> opponentId = userGameInstances.stream()
				.map(MultiplayerUserGameInstance::getUserId)
				.filter(id -> ObjectUtils.notEqual(id, playerId))
				.findFirst();
		
		PlayedDuelInfo duelInfo = new PlayedDuelInfo(gameCreatorId);
		if (opponentId.isPresent()) {
			duelInfo.setOpponentUserId(opponentId.get());
		}
		return duelInfo;
	}
	
	private PlayedTournamentInfo getFinishedTournamentInfo(String mgiId, MultiplayerUserGameInstance userGameInstance, int numberOfParticipants) {
		PlayedTournamentInfo tournamentInfo = new PlayedTournamentInfo(mgiId, userGameInstance.getGameInstanceId());
		tournamentInfo.setOpponentsNumber(numberOfParticipants - 1);
		return tournamentInfo;
	}
	
	public void moveDashboardData(String tenantId, String sourceUserId, String targetUserId) {
		moveDashboardDao.moveLastPlayedGame(tenantId, sourceUserId, targetUserId);
	}

}
