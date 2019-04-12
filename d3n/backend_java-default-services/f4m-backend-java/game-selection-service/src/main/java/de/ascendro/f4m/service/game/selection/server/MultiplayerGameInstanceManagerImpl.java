package de.ascendro.f4m.service.game.selection.server;

import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDao;
import de.ascendro.f4m.server.game.GameAerospikeDao;
import de.ascendro.f4m.server.game.GameResponseSanitizer;
import de.ascendro.f4m.server.game.GameUtil;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.multiplayer.dao.PublicGameElasticDao;
import de.ascendro.f4m.server.multiplayer.move.dao.MoveMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.request.jackpot.PaymentServiceCommunicator;
import de.ascendro.f4m.server.result.dao.CommonResultEngineDao;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.event.store.EventSubscriptionStore;
import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.client.F4MEntryAlreadyExistsException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.game.engine.exception.F4MGameFlowViolation;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.selection.client.NotificationMessagePreparer;
import de.ascendro.f4m.service.game.selection.client.communicator.GameEngineCommunicator;
import de.ascendro.f4m.service.game.selection.config.GameSelectionConfig;
import de.ascendro.f4m.service.game.selection.exception.*;
import de.ascendro.f4m.service.game.selection.model.game.EntryFee;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.*;
import de.ascendro.f4m.service.game.selection.model.schema.GameSelectionMessageSchemaMapper;
import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.OrderBy;
import de.ascendro.f4m.service.json.model.OrderBy.Direction;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.api.ApiProfileExtendedBasicInfo;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.F4MEnumUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.ascendro.f4m.service.game.selection.exception.GameSelectionExceptionCodes.GAME_HASNT_STARTED_YET;
import static de.ascendro.f4m.service.game.selection.exception.GameSelectionExceptionCodes.GAME_IS_OVER;

public class MultiplayerGameInstanceManagerImpl implements MultiplayerGameInstanceManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(MultiplayerGameInstanceManagerImpl.class);

	private final Config config;
	private final CommonMultiplayerGameInstanceDao mgiDao;
	private final NotificationMessagePreparer notificationMessagePreparer;
	private final GameAerospikeDao gameDao;
	private final PaymentServiceCommunicator paymentServiceCommunicator;
	private final GameEngineCommunicator gameEngineCommunicator;
	private final PublicGameElasticDao publicGameElasticDao;
	private final CommonProfileAerospikeDao profileDao;
	private final MoveMultiplayerGameInstanceDao moveMgiDao;
	private final EventSubscriptionStore eventSubscriptionStore;
	private final GameSelectionMessageSchemaMapper gameSelectionMessageSchemaMapper;
	private final CommonResultEngineDao commonResultEngineDao;
	private final CommonGameInstanceAerospikeDao commonGameInstanceAerospikeDao;
	private final GameResponseSanitizer gameResponseSanitizer;
	private final CommonBuddyElasticDao buddyElasticDao;

	@Inject
	public MultiplayerGameInstanceManagerImpl(CommonMultiplayerGameInstanceDao mgiDao,
			NotificationMessagePreparer notificationMessagePreparer, GameAerospikeDao gameDao,
			PaymentServiceCommunicator paymentServiceCommunicator, Config config,
			GameEngineCommunicator gameEngineCommunicator, PublicGameElasticDao publicGameElasticDao,
			CommonProfileAerospikeDao profileDao, MoveMultiplayerGameInstanceDao moveMgiDao,
			EventSubscriptionStore eventSubscriptionStore,
			GameSelectionMessageSchemaMapper gameSelectionMessageSchemaMapper,
			CommonResultEngineDao commonResultEngineDao, CommonGameInstanceAerospikeDao commonGameInstanceAerospikeDao,
			GameResponseSanitizer gameResponseSanitizer, CommonBuddyElasticDao buddyElasticDao) {
		this.mgiDao = mgiDao;
		this.notificationMessagePreparer = notificationMessagePreparer;
		this.gameDao = gameDao;
		this.paymentServiceCommunicator = paymentServiceCommunicator;
		this.config = config;
		this.gameEngineCommunicator = gameEngineCommunicator;
		this.publicGameElasticDao = publicGameElasticDao;
		this.profileDao = profileDao;
		this.moveMgiDao = moveMgiDao;
		this.eventSubscriptionStore = eventSubscriptionStore;
		this.gameSelectionMessageSchemaMapper = gameSelectionMessageSchemaMapper;
		this.commonResultEngineDao = commonResultEngineDao;
		this.commonGameInstanceAerospikeDao = commonGameInstanceAerospikeDao;
		this.gameResponseSanitizer = gameResponseSanitizer;
		this.buddyElasticDao = buddyElasticDao;
	}

	@Override
	public CustomGameConfig createMultiplayerGameInstance(MultiplayerGameParameters multiplayerGameParameters,
			ClientInfo clientInfo, JsonMessage<? extends JsonMessageContent> sourceMessage,
			SessionWrapper sourceSession, boolean rematch, boolean isPublicGame) {

		String userId = clientInfo.getUserId();
		Game game = gameDao.getGame(multiplayerGameParameters.getGameId());
		multiplayerGameParameters.validate(game);
		CustomGameConfig customGameConfig = CustomGameConfigBuilder
				.create(clientInfo.getTenantId(), clientInfo.getAppId(), game.getGameId(), userId)
				.applyMultiplayerGameParameters(multiplayerGameParameters)
				.withPublicGame(isPublicGame)
				.withPlayDateTime(getExpiryDateTime(game, multiplayerGameParameters))
				.withRematch(rematch)
				.applyGame(game, getExpiryDateTime(game, multiplayerGameParameters))
				.withMinimumJackpotGarantie(game.getMinimumJackpotGarantie())
				.build();
        String mgiId = mgiDao.create(userId, customGameConfig);
		if (customGameConfig.getGameType().isDuel() && customGameConfig.getGameType().isMultiUser()) {
			mgiDao.createDuel(userId, customGameConfig, mgiId);//mgiId it is mgiId
		}
		if (!GameUtil.isFreeGame(game, customGameConfig)) {
            if (customGameConfig.getGameType().isTournament()) gameDao.createARecordMgiId(game.getGameId(), mgiId);

			paymentServiceCommunicator.sendCreateJackpotRequest(mgiId, customGameConfig, game, sourceMessage, sourceSession);
		} else if (sourceSession != null && userId != null) {
			gameEngineCommunicator.requestRegister(sourceMessage, sourceSession, mgiId);
		} else {
			// if external request is not sent, save the game to elastic immediately
			addPublicGameToElastic(customGameConfig, game);
			//Note - not clear, why we have to wait for register response, if game is free.
			//Is it just because usual tournaments must have an author registered before showing the game to others? 
		}
		return customGameConfig;
	}

	// FIXME It will be necessary to remake the transfer of funds from the account of the tenant when creating the tournament.
	public void sendTransferMinimumJackpotGuarantee(String mgiId) {
        CustomGameConfig customGameConfig = mgiDao.getConfig(mgiId);
        Game game = gameDao.getGame(customGameConfig.getGameId());
        if (game.getMinimumJackpotGarantie() != null && game.getMinimumJackpotGarantie() > 0
                && (customGameConfig.getEntryFeeCurrency() == Currency.MONEY || !game.getResultConfiguration().isJackpotCalculateByEntryFee())) {
            paymentServiceCommunicator.sendTransferMinimumJackpotGuarantee(customGameConfig, game, null, null, mgiId);
        }
    }

	@Override
	public void setMultiplayerGameInstanceAsExpired(String mgiId) {
		mgiDao.markAsExpired(mgiId);
		mgiDao.markGameAsExpired(mgiId);
	}

	@Override
	public void addPublicGameToElastic(String mgiId) {
		CustomGameConfig customGameConfig = mgiDao.getConfig(mgiId);
		Game game = gameDao.getGame(customGameConfig.getGameId());
		addPublicGameToElastic(customGameConfig, game);
	}

	private void addPublicGameToElastic(CustomGameConfig customGameConfig, Game game) {
		boolean shouldAddToPublicGameList = customGameConfig.getGameType().isTournament()
				|| (customGameConfig.getGameType().isDuel() && Boolean.TRUE.equals(customGameConfig.getPublicGame()));
		if (shouldAddToPublicGameList) {
			publicGameElasticDao.createOrUpdate(Arrays.asList(game.getApplications()),
					new Invitation(customGameConfig, customGameConfig.getGameCreatorId()));
		}
	}

	@Override
	public String createPublicGame(MultiplayerGameParameters params, ClientInfo clientInfo,
			JsonMessage<? extends JsonMessageContent> sourceMessage, SessionWrapper sourceSession) {
		Game game = gameDao.getGame(params.getGameId());
		String instanceId;
		if (game.getType().isDuel()) {
			CustomGameConfig customGameConfig = createMultiplayerGameInstance(params, clientInfo, sourceMessage,
					sourceSession, false, true);
			instanceId = customGameConfig.getId();
		} else {
			throw new F4MGameTypeNotValidException(String
					.format("Only Duel can be created as public game, current game type is [%s]", game.getType()));
		}
		return instanceId;
	}

	private ZonedDateTime getExpiryDateTime(Game game, MultiplayerGameParameters params) {
		ZonedDateTime expiryDateTime;
		if (game.getType().isOneOf(GameType.DUEL, GameType.USER_TOURNAMENT, GameType.USER_LIVE_TOURNAMENT)) {
			Integer minutesToAccept = Optional.ofNullable(game.getTimeToAcceptInvites())
					.orElse(config.getPropertyAsInteger(GameConfigImpl.MINUTES_TO_ACCEPT_INVITE_DEFAULT));
			expiryDateTime = DateTimeUtil.getCurrentDateTime().plusMinutes(minutesToAccept);
		} else {
			expiryDateTime = ObjectUtils.firstNonNull(params.getEndDateTime(), game.getEndDateTime());
		}
		return expiryDateTime;
	}

	@Override
	public CustomGameConfig getMultiplayerGameConfig(String instanceId) {
		return mgiDao.getConfig(instanceId);
	}

	@Override
	public List<String> addUsers(List<String> inviteesIds, String instanceId, String inviterId) {
		List<String> invitedUsers = new ArrayList<>();
		for (String inviteeId : inviteesIds) {
			if (addUser(inviteeId, instanceId, inviterId)) {
				invitedUsers.add(inviteeId);
			}
		}
		return invitedUsers;
	}

	@Override
	public boolean addUser(String inviteeId, String instanceId, String inviterId) {
		boolean result;
		try {
			mgiDao.addUser(instanceId, inviteeId, inviterId, MultiplayerGameInstanceState.PENDING);
			result = true;
		} catch (F4MEntryAlreadyExistsException e) {
			result = false;
			LOGGER.info("User [{}] has already been invited", inviteeId, e);
		}
		return result;
	}

	@Override
	public void activateInvitationsAndSendInviteNotifications(String mgiId, ClientInfo clientInfo) {
		List<String> invitedUsers = mgiDao.activateInvitations(mgiId);
		for (String inviteeId : invitedUsers) {
			String inviterId = mgiDao.getInviterId(inviteeId, mgiId);
			ClientInfo inviteeClientInfo = buildClientInfo(clientInfo, inviteeId);
			notificationMessagePreparer.sendInviteNotification(inviterId, inviteeId, mgiId, inviteeClientInfo);
			notificationMessagePreparer.sendInvitationExpirationNotification(inviteeId, inviterId, mgiId,
					inviteeClientInfo);
		}
	}

	private ClientInfo buildClientInfo(ClientInfo clientInfo, String inviteeId) {
		if (clientInfo != null) {
			ClientInfo inviteeClientInfo = ClientInfo.cloneOf(clientInfo);
			Objects.requireNonNull(inviteeClientInfo).setUserId(inviteeId);
			inviteeClientInfo.setIp(null);
			return inviteeClientInfo;
		} else {
			return null;
		}
	}

	@Override
	public void inviteUserIfNotExist(String userId, String mgiId, GameType gameType) {
		// number of player attempts in this mgi(tournament)
		Long numberOfAttemptsUser = mgiDao.numberOfAttempts(mgiId, userId);
		// increased number of attempts to (game in tournament) until three.
		// so far only for the tipp tournament, in the future for all tournaments.

		if (mgiDao.getUserState(mgiId, userId) == null /* gameType.isTournament()GameType.TIPP_TOURNAMENT == gameType && numberOfAttemptsUser < 3)*/) {
			/* if (GameType.TIPP_TOURNAMENT == gameType && gameType.isTournament()) {
				mgiDao.createOrUpdateTournamentLong(mgiId, userId, mgiDao.NUMBER_OF_ATTEMPTS, ++numberOfAttemptsUser);
			}*/
			String inviterId = getGameCreatorId(mgiId);
			if (gameType.isTournament() && inviterId == null) {
				inviterId = "";
			} else if (inviterId == null) {
				inviterId = userId;
			}
			mgiDao.addUser(mgiId, userId, inviterId, MultiplayerGameInstanceState.INVITED);
		} else
			throw new GameAlreadyPlayedByUserException("You can only play once.");
	}

	@Override
	public String getInviterId(String userId, String mgiId) {
		return mgiDao.getInviterId(userId, mgiId);
	}

	@Override
	public void declineInvitation(String userId, String mgiId, ClientInfo clientInfo) {
		long capacityId = mgiDao.getTotalRecordCountDuel(mgiId);
        CustomGameConfig customGameConfig = mgiDao.getConfig(mgiId);
		if (capacityId > 1 && customGameConfig.getGameType().isMultiUser()) {
			mgiDao.updateDuel(userId, customGameConfig, mgiId, capacityId);
		} else if (!customGameConfig.isFree() && customGameConfig.getGameType().isMultiUser()) {
			mgiDao.deleteDuel(mgiId);
            paymentServiceCommunicator.refundPayment(mgiId, customGameConfig);
		}
		String inviterId = mgiDao.getInviterId(userId, mgiId, MultiplayerGameInstanceState.INVITED);
		if (inviterId != null) {
			notificationMessagePreparer.sendInvitationResponseNotification(userId, false, inviterId, mgiId, clientInfo);
			cancelScheduledNotification(mgiId, userId, clientInfo);
		}
		mgiDao.declineInvitation(userId, mgiId);
	}

	@Override
	public void rejectInvitation(String userId, String mgiId, ClientInfo clientInfo) {
		mgiDao.rejectInvitation(userId, mgiId);
		cancelScheduledNotification(mgiId, userId, clientInfo);
	}

	@Override
	public void acceptedInvitation(String userId, String mgiId, ClientInfo clientInfo) {
		String inviterId = mgiDao.getInviterId(userId, mgiId, MultiplayerGameInstanceState.REGISTERED);
		if (inviterId != null) {
			notificationMessagePreparer.sendInvitationResponseNotification(userId, true, inviterId, mgiId, clientInfo);
		}

		CustomGameConfig customGameConfig = mgiDao.getConfig(mgiId);
		Game game = gameDao.getGame(customGameConfig.getGameId());
		if (game.isDuel()) {
			mgiDao.deleteNotAcceptedInvitations(mgiId);
			cancelScheduledNotifications(mgiId, clientInfo);
		} else {
			//on USER TOURNAMENTS cancel only own notification
			cancelScheduledNotification(mgiId, userId, clientInfo);
		}
	}

	@Override
	public List<Invitation> getInvitationList(String tenantId, String appId, String userId, List<String> states,
			CreatedBy createdBy, FilterCriteria filterCriteria) {
		List<MultiplayerGameInstanceState> mgiStates = states.stream()
				.map(s -> F4MEnumUtils.getEnum(MultiplayerGameInstanceState.class, s)).collect(Collectors.toList());
		return mgiDao.getInvitationList(tenantId, appId, userId, mgiStates, createdBy, filterCriteria);
	}

	@Override
	public List<Invitation> filterInvitationsByResults(List<Invitation> invitations, boolean isPending) {
		Predicate<Invitation> hasResults = i -> commonResultEngineDao
				.hasMultiplayerResults(i.getMultiplayerGameInstanceId());
		return invitations.stream().filter(isPending ? hasResults.negate() : hasResults).collect(Collectors.toList());
	}

	@Override
	public void addInvitationOpponents(List<Invitation> invitations, String inviterId) {
		for (Invitation invitation : invitations) {
			String mgiId = invitation.getMultiplayerGameInstanceId();
			List<InvitedUser> invitedList = getInvitedList(mgiId, Integer.MAX_VALUE, 0, Collections.emptyList(),
					Collections.emptyMap());
			invitedList.stream().filter(u -> !Objects.equals(u.getUserId(), inviterId) && hasUserResponded(u))
					.forEach(u -> addOpponentToInvitation(u.getUserId(), invitation));
		}
	}

	private boolean hasUserResponded(InvitedUser user) {
		MultiplayerGameInstanceState state = F4MEnumUtils.getEnum(MultiplayerGameInstanceState.class, user.getStatus());
		return Objects.requireNonNull(state).isOpponent(state);
	}

	private void addOpponentToInvitation(String userId, Invitation invitation) {
		ApiProfileExtendedBasicInfo opoonent = profileDao.getProfileExtendedBasicInfo(userId);
		invitation.addOpponent(opoonent);
	}

	@Override
	public void addGameInfo(List<Invitation> invitations) {
		List<Invitation> copy = new ArrayList<>(invitations);
		copy.stream().filter(i -> i.getGame().getId() != null).forEach(invitation -> {
				try {
					InvitationGame invitationGame = invitation.getGame();
					Game game = gameResponseSanitizer.removeExcessInfo(gameDao.getGame(invitationGame.getId()));
					invitationGame.setAssignedPools(game.getAssignedPools());
					invitationGame.setAssignedPoolsColors(game.getAssignedPoolsColors());
					invitationGame.setAssignedPoolsNames(game.getAssignedPoolsNames());
					invitationGame.setAssignedPoolsIcons(game.getAssignedPoolsIcons());

					invitation.setShortPrize(game.getShortPrize());
				} catch (F4MEntryNotFoundException e) {
					LOGGER.error(
							"Error while processing invitation for (mgiId=[{}] and gameId=[{}]); skipping this item. Cause: [{}]",
							invitation.getMultiplayerGameInstanceId(), invitation.getGame().getId(), e);
					invitations.remove(invitation);
				}
			});
	}

	@Override
	public void addGameInstanceInfo(List<Invitation> invitations, String userId) {
		invitations.stream()
			.filter(i -> i.getMultiplayerGameInstanceId() != null)
			.forEach(i -> {
				String gameInstanceId = mgiDao.getGameInstanceId(i.getMultiplayerGameInstanceId(), userId);
				GameStatus status = null;
				if(gameInstanceId != null) {
					GameInstance gameInstance = commonGameInstanceAerospikeDao.getGameInstance(gameInstanceId);
					status = gameInstance.getGameState().getGameStatus();
				}

				i.setGameInstanceId(gameInstanceId);
				i.setGameInstance(new GameInstanceInfo(gameInstanceId, status == null ? null : status.toString()));
			});
	}

	@Override
	public void addInvitationUserInfo(List<Invitation> invitations, Function<Invitation, String> getUserId,
			BiConsumer<Invitation, ApiProfileExtendedBasicInfo> setUserInfo) {
		List<String> profileIds = invitations.stream()
				.map(getUserId)
        		.distinct()
        		.filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<String, ApiProfileExtendedBasicInfo> infoMap = profileDao.getProfileExtendedBasicInfo(profileIds);

		invitations.forEach(invitation -> {
			ApiProfileExtendedBasicInfo userInfo = infoMap.get(getUserId.apply(invitation));
			if (userInfo != null) {
				setUserInfo.accept(invitation, userInfo);
			}
		});
	}

	@Override
	public List<InvitedUser> getInvitedList(String mgiId, int limit, long offset, List<OrderBy> orderBy,
			Map<String, String> searchBy) {
		return mgiDao.getInvitedList(mgiId, limit, offset, orderBy, searchBy);
	}

	@Override
	public Set<String> getGamesUserHasBeenInvitedTo(String tenantId, String appId, String userId) {
		return mgiDao.getGamesInvitedTo(tenantId, appId, userId);
	}

	@Override
	public boolean validateParticipantCount(String mgiId, String userId) {
		if (!mgiDao.hasValidParticipantCount(mgiId)) {
			throw new F4MGameParticipantCountExceededException(
					String.format("Max registered user count has been reached (mgiId=[%s], userId=[%s])", mgiId, userId));
		}
		return true;
	}

	@Override
	public boolean validateConfirmInvitation(String mgiId, String userId) {
		MultiplayerGameInstanceState userState = mgiDao.getUserState(mgiId, userId);
		if (userState == null) {
			throw new F4MEntryNotFoundException(String.format("User is not invited to MGI [%s]", mgiId));
		} else if (MultiplayerGameInstanceState.INVITED != userState) {
			throw new F4MGameFlowViolation(
					String.format("User cannot confirm invitation to MGI [%s] with user state [%s]", mgiId, userState));
		}
		return true;
	}

	@Override
	public boolean validateInvitation(MultiplayerGameParameters multiplayerGameParameters, String mgiId, List<?> invitees) {
		int inviteeCount = Optional.ofNullable(invitees).orElse(Collections.emptyList()).size();
		GameType gameType = getGameType(multiplayerGameParameters, mgiId);
		switch (gameType) {
		case DUEL:
			if (mgiId != null) {
				throw new F4MGameInvitationNotValidException(String.format("Cannot invite users to created Duel [%s]", mgiId));
			}
			int duelInviteeMaxCount = config.getPropertyAsInteger(GameSelectionConfig.DUEL_INVITEE_MAX_COUNT);
			if (inviteeCount > duelInviteeMaxCount) {
				throw new F4MGameInvitationNotValidException(String.format("Cannot invite more than %d users to Duel", duelInviteeMaxCount));
			}
			break;
		case QUIZ24:
			throw new F4MGameTypeNotValidException("Cannot invite users to Quiz game");
		default:
			// Add other type validations
			break;
		}
		return true;
	}

	@Override
	public GameType getGameType(final MultiplayerGameParameters multiplayerGameParameters, final String mgiId) {
		GameType gameType = null;
		if (multiplayerGameParameters != null && StringUtils.isNotEmpty(multiplayerGameParameters.getGameId())) {
			gameType = gameDao.getGame(multiplayerGameParameters.getGameId()).getType();
		} else if (StringUtils.isNotEmpty(mgiId)) {
			gameType = mgiDao.getConfig(mgiId).getGameType();
		}
		if (gameType == null) {
			throw new F4MGameInvitationNotValidException("Cannot obtain game type");
		}
		return gameType;
	}

	private String getGameCreatorId(final String mgiId) {
		return mgiDao.getConfig(mgiId).getGameCreatorId();
	}

	@Override
	public boolean validateUserState(String userId, String mgiId) {
		if (!eventSubscriptionStore.hasSubscription(mgiId)) {
			throw new SubscriptionNotFoundException(String.format("There is not start/count down topic for MGI [%s]", mgiId));
		} else {
			MultiplayerGameInstanceState userState = mgiDao.getUserState(mgiId, userId);
			if (userState == MultiplayerGameInstanceState.STARTED) {
				throw new GameAlreadyStartedByUserException(String.format("MGI [%s] state is not started", mgiId));
			} else if (userState != MultiplayerGameInstanceState.REGISTERED) {
				throw new F4MGameFlowViolation(String.format("User cannot receive game start notificaiton at current state [%s]", userState));
			}
		}
		return true;
	}

	@Override
	public void validateUserPermissionsForEntryFee(String messageName, ClientInfo clientInfo, String mgiId) {
		CustomGameConfig entryFee = mgiDao.getConfig(mgiId);
		if (entryFee != null) {
			validateUserPermissionsForEntryFee(messageName, clientInfo, entryFee);
		} else {
			throw new F4MEntryNotFoundException(String.format("Multiplayer game instance not found [%s]", mgiId));
		}
	}

	public void validateIsGameHasStartedDateTime(ClientInfo clientInfo, String mgiId) {
		CustomGameConfig config = mgiDao.getConfig(mgiId);
		if (config != null && config.getGameType().isTournament()) {
			if (DateTimeUtil.getCurrentDateTime().compareTo(config.getStartDateTime()) < 0) {
				throw new F4MGameHasntStartedYetOrIsOverException(GAME_HASNT_STARTED_YET, String.format("Multiplayer game instance hasn't started yet [%s]", mgiId));
			} else if (DateTimeUtil.getCurrentDateTime().compareTo(config.getEndDateTime()) > 0) {
				throw new F4MGameHasntStartedYetOrIsOverException(GAME_IS_OVER, String.format("Multiplayer game instance is over [%s]", mgiId));
			}
		}
	}


	@Override
	public void validateUserPermissionsForEntryFee(String messageName, ClientInfo clientInfo, EntryFee entryFee) {
		Profile profile = profileDao.getProfile(clientInfo.getUserId());
		GameUtil.validateUserAgeForEntryFee(entryFee, profile);
		if (ArrayUtils.isNotEmpty(clientInfo.getRoles())) {
			Set<String> messagePermissions = gameSelectionMessageSchemaMapper.getMessagePermissions(messageName);
			Set<String> userPermissions = gameSelectionMessageSchemaMapper.getRolePermissions(clientInfo.getRoles());
			userPermissions.retainAll(messagePermissions);
			GameUtil.validateUserPermissionsForEntryFee(entryFee, userPermissions);
		} else {
			throw new F4MInsufficientRightsException("User roles required");
		}
	}

	@Override
	public Invitation getInvitation(String tenantId, String appId, String userId, GameType gameType) {
		FilterCriteria filterCriteria = new FilterCriteria();
		filterCriteria.setLimit(1);
		filterCriteria.addOrderBy(new OrderBy(CustomGameConfig.PROPERTY_EXPIRY_DATE, Direction.asc));
		filterCriteria.addSearchBy(CustomGameConfig.PROPERTY_GAME_TYPE, gameType.name());
		List<Invitation> invitations = getInvitationList(tenantId, appId, userId,
				Collections.singletonList(MultiplayerGameInstanceState.INVITED.name()), CreatedBy.OTHERS, filterCriteria);
		return invitations.isEmpty() ? new Invitation() : invitations.get(0);
	}

	@Override
	public void updateLastInvitedAndPausedDuelOpponents(String mgiId, String inviterId, List<String> invitedUsers,
			List<String> pausedUsers) {
        CustomGameConfig customGameConfig = mgiDao.getConfig(mgiId);
		Game game = gameDao.getGame(customGameConfig.getGameId());
		if (game.isDuel()) {
			profileDao.setLastInvitedDuelOponents(inviterId, customGameConfig.getAppId(), invitedUsers);
			profileDao.setPausedDuelOponents(inviterId, customGameConfig.getAppId(), pausedUsers);
		}
	}

	@Override
	public void mapTournament(String gameId, String mgiId) {
		mgiDao.mapTournament(gameId, mgiId);
	}

	@Override
	public String getTournamentInstanceId(String gameId) {
		return mgiDao.getTournamentInstanceId(gameId);
	}

	@Override
	public List<MultiplayerUserGameInstance> getMultiplayerUserGameInstances(String mgiId, MultiplayerGameInstanceState... states) {
		return mgiDao.getGameInstances(mgiId, states);
	}

	@Override
	public List<Invitation> listPublicGames(PublicGameFilter filter, int limit) {
		return publicGameElasticDao.searchPublicGames(filter, limit);
	}

	@Override
	public long getNumberOfPublicGames(PublicGameFilter publicGameFilter) {
		return publicGameElasticDao.getNumberOfPublicGames(publicGameFilter);
	}

	@Override
	public Invitation getNextPublicGame(PublicGameFilter publicGameFilter) {
        return publicGameElasticDao.getNextPublicGame(publicGameFilter);
    }

	@Override
	public void deletePublicGameSilently(String mgiId) {
		publicGameElasticDao.delete(mgiId, true, true);
	}

	@Override
	public Invitation getPublicGame(String appId, String mgiId) {
		return publicGameElasticDao.getPublicGame(appId, mgiId);
	}

	@Override
	public void moveMultiplayerGameInstanceData(String tenantId, String sourceUserId, String targetUserId) {
		moveMgiDao.moveUserMgiData(tenantId, null, sourceUserId, targetUserId);
	}

	@Override
	public void movePublicGameData(String sourceUserId, String targetUserId) {
		publicGameElasticDao.changeUserOfPublicGame(sourceUserId, targetUserId);
	}

	@Override
	public long getMillisToPlayDateTime(String mgiId) {
		ZonedDateTime playDateTime = getPlayDateTime(mgiId);
		ZonedDateTime now = DateTimeUtil.getCurrentDateTime();
		long secondsToPlayDateTime = 0;
		if (playDateTime != null && now.isBefore(playDateTime)) {
			secondsToPlayDateTime = DateTimeUtil.getSecondsBetween(now, playDateTime);
		}
		return secondsToPlayDateTime * 1000;
	}

	@Override
	public ZonedDateTime getPlayDateTime(String mgiId) {
		CustomGameConfig multiplayerGameConfig = getMultiplayerGameConfig(mgiId);
		ZonedDateTime playDateTime;
		if (multiplayerGameConfig.getGameType().isLive()) {
			playDateTime = multiplayerGameConfig.getPlayDateTime();
		} else {
			playDateTime = multiplayerGameConfig.getStartDateTime();
		}
		return playDateTime;
	}

	@Override
	public void setScheduledNotification(String mgiId, String userId, String notificationId) {
		if (notificationId != null) {
			mgiDao.setInvitationExpirationNotificationId(mgiId, userId, notificationId);
		}
	}

	@Override
	public void cancelScheduledNotification(String mgiId, String userId, ClientInfo clientInfo) {
		String notificationId = mgiDao.getInvitationExpirationNotificationId(mgiId, userId);
		if (notificationId != null) {
			notificationMessagePreparer.cancelInvitationExpirationNotification(notificationId, userId, mgiId, clientInfo);
		}
	}

	@Override
	public void resetScheduledNotification(String mgiId, String userId) {
		mgiDao.resetInvitationExpirationNotificationId(mgiId, userId);
	}

	private void cancelScheduledNotifications(String mgiId, ClientInfo clientInfo) {
		Map<String, String> usersMap = mgiDao.getAllUsersOfMgi(mgiId);
		usersMap.keySet().stream().forEach(userId -> {
			ClientInfo userClientInfo = buildClientInfo(clientInfo, userId);
			cancelScheduledNotification(mgiId, userId, userClientInfo);
		});
	}

	@Override
	public List<String> ensureHasNoBlocking(String userId, List<String> invitedUsersIds, boolean throwException) {
		if (invitedUsersIds == null) {
			return Collections.emptyList();
		}
		final List<String> result = new ArrayList<>(invitedUsersIds.size());
		invitedUsersIds.forEach(invitedUserId -> {
			if (buddyElasticDao.isBlockingMe(userId, invitedUserId)) {
				if (throwException) {
					throw new F4MGameInvitationNotValidException("Trying to invite user (" + invitedUserId +
							") who is blocking invitation requests from current user (" + userId + ")");
				}
			} else {
				result.add(invitedUserId);
			}
		});
		return result;
	}

}
