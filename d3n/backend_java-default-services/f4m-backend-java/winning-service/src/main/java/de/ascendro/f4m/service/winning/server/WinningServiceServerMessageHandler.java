package de.ascendro.f4m.service.winning.server;

import com.google.gson.JsonObject;
import de.ascendro.f4m.server.analytics.model.RewardEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.game.GameAerospikeDao;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.winning.WinningMessageTypes;
import de.ascendro.f4m.service.winning.client.PaymentServiceCommunicator;
import de.ascendro.f4m.service.winning.client.ResultEngineCommunicator;
import de.ascendro.f4m.service.winning.client.VoucherServiceCommunicator;
import de.ascendro.f4m.service.winning.client.WinningComponentMoneyCheckRequestInfo;
import de.ascendro.f4m.service.winning.exception.F4MComponentAlreadyUsedException;
import de.ascendro.f4m.service.winning.exception.F4MComponentNotAvailableException;
import de.ascendro.f4m.service.winning.manager.WinningComponentManager;
import de.ascendro.f4m.service.winning.manager.WinningManager;
import de.ascendro.f4m.service.winning.model.*;
import de.ascendro.f4m.service.winning.model.component.*;
import de.ascendro.f4m.service.winning.model.winning.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Winning Service Jetty Server websocket message handler
 */
public class WinningServiceServerMessageHandler extends DefaultJsonMessageHandler {

	private final ResultEngineCommunicator resultEngineCommunicator;
	private final VoucherServiceCommunicator voucherServiceCommunicator;
	private final PaymentServiceCommunicator paymentServiceCommunicator;
	private final WinningComponentManager winningComponentManager;
	private final WinningManager winningManager;
	private final Tracker tracker;
	private final CommonProfileAerospikeDao profileDao;
	private final GameAerospikeDao gameDao;
	private final CommonGameInstanceAerospikeDao gameInstanceDao;

	public WinningServiceServerMessageHandler(ResultEngineCommunicator resultEngineCommunicator, WinningComponentManager winningComponentManager,
											  WinningManager winningManager, VoucherServiceCommunicator voucherServiceCommunicator,
											  PaymentServiceCommunicator paymentServiceCommunicator, CommonProfileAerospikeDao profileDao, Tracker tracker,
											  CommonGameInstanceAerospikeDao gameInstanceDao, GameAerospikeDao gameDao)
	{
		this.resultEngineCommunicator = resultEngineCommunicator;
		this.winningComponentManager = winningComponentManager;
		this.winningManager = winningManager;
		this.voucherServiceCommunicator = voucherServiceCommunicator;
		this.paymentServiceCommunicator = paymentServiceCommunicator;
		this.tracker = tracker;
		this.profileDao = profileDao;
		this.gameInstanceDao = gameInstanceDao;
		this.gameDao = gameDao;

	}

	@Override
	public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> message) throws F4MException {
		final WinningMessageTypes winningMessageType = message.getType(WinningMessageTypes.class);
		if (winningMessageType != null) {
			return onWinningMessage(message, winningMessageType);
		} else {
			throw new F4MValidationFailedException("Message type must not be null");
		}
	}
	
	@SuppressWarnings("unchecked")
	public JsonMessageContent onWinningMessage(JsonMessage<? extends JsonMessageContent> message, WinningMessageTypes winningMessageType) {
		switch (winningMessageType) {
		case WINNING_COMPONENT_GET:
			return onWinningComponentGet((JsonMessage<WinningComponentGetRequest>) message);
		case WINNING_COMPONENT_LIST:
			return onWinningComponentList((JsonMessage<WinningComponentListRequest>) message);
		case USER_WINNING_COMPONENT_ASSIGN:
			return onUserWinningComponentAssign((JsonMessage<UserWinningComponentAssignRequest>) message);
		case USER_WINNING_COMPONENT_LIST:
			return onUserWinningComponentList((JsonMessage<UserWinningComponentListRequest>) message);
		case USER_WINNING_COMPONENT_GET:
			return onUserWinningComponentGet((JsonMessage<UserWinningComponentGetRequest>) message);
		case USER_WINNING_COMPONENT_LOAD:
			return onUserWinningComponentLoad((JsonMessage<UserWinningComponentLoadRequest>) message);
		case USER_WINNING_COMPONENT_START:
			return onUserWinningComponentStart((JsonMessage<UserWinningComponentStartRequest>) message);
		case USER_WINNING_COMPONENT_STOP:
			return onUserWinningComponentStop((JsonMessage<UserWinningComponentStopRequest>) message);
		case USER_WINNING_COMPONENT_MOVE:
			return onUserWinningComponentMove((JsonMessage<UserWinningComponentMoveRequest>) message);
		case USER_WINNING_LIST:
			return onUserWinningList((JsonMessage<UserWinningListRequest>) message);
		case USER_WINNING_GET:
			return onUserWinningGet((JsonMessage<UserWinningGetRequest>) message);
		case USER_WINNING_MOVE:
			return onUserWinningMove((JsonMessage<UserWinningMoveRequest>) message);
		default:
			throw new F4MValidationFailedException("Unsupported message type[" + winningMessageType + "]");
		}
	}

	private JsonMessageContent onWinningComponentGet(JsonMessage<WinningComponentGetRequest> message) {
		final String tenantId = message.getClientInfo().getTenantId();
		final WinningComponentGetRequest request = message.getContent();
		final String winningComponentId = request.getWinningComponentId();
		final WinningComponent winningComponent = winningComponentManager.getWinningComponent(tenantId, winningComponentId);
		final Game game;
		if (StringUtils.isNotBlank(request.getGameInstanceId())) {
			game = gameInstanceDao.getGameByInstanceId(request.getGameInstanceId());
		} else if (StringUtils.isNotBlank(request.getGameId())) {
			game = gameDao.getGame(request.getGameId());
		} else {
			game = null;
		}
		BigDecimal maxJackpotAmount = BigDecimal.ZERO;
		String maxJackpotType = "";
		if (winningComponent != null && winningComponent.getWinningComponentId()!=null){
			MaxJackpot winningComponentsMaxJackpot= new MaxJackpot();
			winningComponentsMaxJackpot.addWinningComponentJackpot(winningComponent);
			maxJackpotAmount=winningComponentsMaxJackpot.getAmount();
			maxJackpotType=winningComponentsMaxJackpot.getCurrency();
		}
		Objects.requireNonNull(winningComponent).setJackpotAmount(maxJackpotAmount);
		winningComponent.setJackpotType(maxJackpotType);
		return new WinningComponentGetResponse(winningComponent, game == null ? null : game.getWinningComponent(winningComponent.getWinningComponentId()));
	}

	private JsonMessageContent onWinningComponentList(JsonMessage<WinningComponentListRequest> message) {
		final String tenantId = message.getClientInfo().getTenantId();
		final WinningComponentListRequest request = message.getContent();
		request.validateFilterCriteria(WinningComponentListRequest.MAX_LIST_LIMIT);
		final Game game;
		if (StringUtils.isNotBlank(request.getGameInstanceId())) {
			game = gameInstanceDao.getGameByInstanceId(request.getGameInstanceId());
			if (game == null || ! StringUtils.equals(tenantId, game.getTenantId())) {
				throw new F4MEntryNotFoundException("Game for game instance ID " + request.getGameInstanceId() + " not found");
			}
		} else {
			game = gameDao.getGame(request.getGameId());
			if (game == null || ! StringUtils.equals(tenantId, game.getTenantId())) {
				throw new F4MEntryNotFoundException("Game ID " + request.getGameId() + " not found");
			}
		}

		final List<ApiWinningComponent> listItems = game.getWinningComponents() == null
				? Collections.emptyList()
				: Arrays.stream(game.getWinningComponents())
						.skip(request.getOffset())
						.limit(request.getLimit())
						.map(winningComponentConfiguration -> {
							final WinningComponent winningComponent = winningComponentManager.getWinningComponent(
									tenantId, winningComponentConfiguration.getWinningComponentId());
							return winningComponent == null ? null
									: new ApiWinningComponent(winningComponent, winningComponentConfiguration);
						})
						.filter(Objects::nonNull)
						.collect(Collectors.toList());

		return new WinningComponentListResponse(request.getLimit(), request.getOffset(),
				ArrayUtils.getLength(game.getWinningComponents()), listItems);
	}

	private JsonMessageContent onUserWinningComponentAssign(JsonMessage<? extends UserWinningComponentAssignRequest> message)
	{
		final String gameInstanceId = message.getContent().getGameInstanceId();
		GameInstance gameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		if ("PAID".equals(message.getContent().getType().name())) {
			if (gameInstance.getUserWinningComponentId(message.getContent().getType().name()) != null) {
				return new UserWinningResponse("PAID winning component already bought!");
			}
		}
		resultEngineCommunicator.requestGameInstanceResults(message, getSessionWrapper(), gameInstanceId);
		return null;
	}

	private JsonMessageContent onUserWinningComponentList(JsonMessage<UserWinningComponentListRequest> message) {
		final UserWinningComponentListRequest content = message.getContent();
		content.validateFilterCriteria(UserWinningComponentListRequest.MAX_LIST_LIMIT);

		List<JsonObject> userWinningComponents = winningComponentManager.getUserWinningComponents(message.getClientInfo().getAppId(), 
				message.getClientInfo().getUserId(), content.getLimit(), content.getOffset(), content.getOrderBy());
		return new UserWinningComponentListResponse(content.getLimit(), content.getOffset(),
				userWinningComponents.size(), userWinningComponents);
	}

	private JsonMessageContent onUserWinningComponentGet(JsonMessage<UserWinningComponentGetRequest> message) {
		String userWinningComponentId = message.getContent().getUserWinningComponentId();
		UserWinningComponent userWinningComponent = winningComponentManager.getUserWinningComponent(
				message.getClientInfo().getAppId(), message.getClientInfo().getUserId(), userWinningComponentId);
		if (userWinningComponent == null) {
			throw new F4MEntryNotFoundException();
		}
		return new UserWinningComponentGetResponse(userWinningComponent);
	}
	
	private JsonMessageContent onUserWinningComponentLoad(JsonMessage<UserWinningComponentLoadRequest> message) {
		String userWinningComponentId = message.getContent().getUserWinningComponentId();
		UserWinningComponent userWinningComponent = winningComponentManager.getUserWinningComponent(
				message.getClientInfo().getAppId(), message.getClientInfo().getUserId(), userWinningComponentId);
		if (userWinningComponent != null) {
			if (UserWinningComponentStatus.USED != userWinningComponent.getStatus()) {
				WinningComponent winningComponent = winningComponentManager.getWinningComponent(
						message.getClientInfo().getTenantId(), userWinningComponent.getWinningComponentId());
				return new UserWinningComponentLoadResponse(winningComponent);
			} else {
				throw new F4MComponentAlreadyUsedException("User winning component is already used");
			}
		} else {
			throw new F4MEntryNotFoundException("User winning component not found (userWinningComponentId=[" + userWinningComponentId + "])");
		}
	}

	private JsonMessageContent onUserWinningComponentStart(JsonMessage<UserWinningComponentStartRequest> message) {
		final String tenantId = message.getClientInfo().getTenantId();
		final String appId = message.getClientInfo().getAppId();
		final String userId = message.getClientInfo().getUserId();
		final String userWinningComponentId = message.getContent().getUserWinningComponentId();
		UserWinningComponent userWinningComponent = winningComponentManager.getUserWinningComponent(appId, userId, userWinningComponentId);
		if (userWinningComponent == null) {
			throw new F4MEntryNotFoundException("Component with ID " + userId + ", " + userWinningComponentId + " not found");
		}
		if (userWinningComponent.getStatus() != UserWinningComponentStatus.NEW) {
			throw new F4MComponentAlreadyUsedException("Component with ID " + userId + ", " + userWinningComponentId + " already used");
		}
		WinningComponent winningComponent = winningComponentManager.getWinningComponent(tenantId, userWinningComponent.getWinningComponentId());
		// First, mark as used - this will throw exception, if already used in the meantime
		winningComponentManager.markUserWinningComponentUsed(appId, userId, userWinningComponent.getUserWinningComponentId());
		
		// Then, determine winning
		WinningOption winning = winningManager.determineWinning(userId, winningComponent);
		if (winning != null) {
			if(winning.getType() == WinningOptionType.MONEY || winning.getType() == WinningOptionType.SUPER) {
				WinningComponentMoneyCheckRequestInfo requestInfo = new WinningComponentMoneyCheckRequestInfo(userWinningComponentId, winningComponent.getWinningComponentId(), winning.getWinningOptionId());
				paymentServiceCommunicator.requestTenantBalance(message, getSessionWrapper(), requestInfo);
				return null;
			}
			saveRewardEvent(message.getClientInfo(), winning, userWinningComponent);
			winningComponentManager.saveUserWinningComponentWinningOption(appId, userId, userWinningComponent.getUserWinningComponentId(), winning);
		}
		return new UserWinningComponentStartResponse(winning);
	}
	
	private JsonMessageContent onUserWinningComponentStop(JsonMessage<UserWinningComponentStopRequest> message) {
		final String appId = message.getClientInfo().getAppId();
		final String tenantId = message.getClientInfo().getTenantId();
		final String userId = message.getClientInfo().getUserId();
		final String userWinningComponentId = message.getContent().getUserWinningComponentId();
		UserWinningComponent userWinningComponent = winningComponentManager.getUserWinningComponent(appId, userId,
				userWinningComponentId);
		if (userWinningComponent == null) {
			throw new F4MEntryNotFoundException("Component with ID " + userId + ", " + userWinningComponentId + " not found");
		}
		if (userWinningComponent.getStatus() != UserWinningComponentStatus.USED) {
			throw new F4MComponentNotAvailableException("Component with ID " + userId + ", " + userWinningComponentId + 
					" cannot be stopped, since it is not started");
		}
		if (userWinningComponent.getWinning().getType() == WinningOptionType.VOUCHER) {
			voucherServiceCommunicator.requestUserVoucherAssign(message, getSessionWrapper(), userWinningComponent,
					tenantId, appId);
		} else {
			paymentServiceCommunicator.requestWinningTransfer(message, getSessionWrapper(), userWinningComponent.getGameId(),
					userWinningComponent);
		}
		return null;
	}
	
	private JsonMessageContent onUserWinningComponentMove(JsonMessage<UserWinningComponentMoveRequest> message) {
		Profile profile = profileDao.getProfile(message.getContent().getSourceUserId());
		if (profile == null) {
			profile = profileDao.getProfile(message.getContent().getTargetUserId());
		}
		winningComponentManager.moveWinningComponents(message.getContent().getSourceUserId(), message.getContent().getTargetUserId(), profile.getApplications());
		return new EmptyJsonMessageContent();
	}

	private JsonMessageContent onUserWinningList(JsonMessage<UserWinningListRequest> message) {
		UserWinningListRequest content = message.getContent();
		content.validateFilterCriteria(UserWinningListRequest.MAX_LIST_LIMIT);

		List<JsonObject> userWinnings = winningManager.getUserWinnings(message.getClientInfo().getAppId(),
				message.getClientInfo().getUserId(), content.getLimit(), content.getOffset(), content.getOrderBy());
		return new UserWinningListResponse(content.getLimit(), content.getOffset(), userWinnings.size(), userWinnings);
	}

	private JsonMessageContent onUserWinningGet(JsonMessage<UserWinningGetRequest> message) {
		final String userWinningId = message.getContent().getUserWinningId();
		final UserWinning userWinning = winningManager.getUserWinning(message.getClientInfo().getAppId(), userWinningId);
		if (userWinning != null) {
			return new UserWinningGetResponse(userWinning);
		} else {
			throw new F4MEntryNotFoundException("User Winning not found");
		}
	}

	private JsonMessageContent onUserWinningMove(JsonMessage<UserWinningMoveRequest> message) {
		Profile profile = profileDao.getProfile(message.getContent().getSourceUserId());
		if (profile == null) {
			profile = profileDao.getProfile(message.getContent().getTargetUserId());
		}
		winningManager.moveWinnings(message.getContent().getSourceUserId(), message.getContent().getTargetUserId(), profile.getApplications());
		return new EmptyJsonMessageContent();
	}

	private void saveRewardEvent(ClientInfo clientInfo, WinningOption winning, UserWinningComponent userWinningComponent) {
		RewardEvent rewardEvent =  new RewardEvent();
		final WinningOptionType winningOptionType = winning.getType();
		if (userWinningComponent.getType() == WinningComponentType.PAID) {
			rewardEvent.setPaidWinningComponentsPlayed(true);
		} else if (userWinningComponent.getType() == WinningComponentType.FREE) {
			rewardEvent.setFreeWinningComponentsPlayed(true);
		}

		if (winningOptionType == WinningOptionType.SUPER) {
			rewardEvent.setSuperPrizeWon(true);
		} else if (winningOptionType == WinningOptionType.CREDITS) {
			rewardEvent.setCreditWon(winning.getAmount().longValue());
		} else if (winningOptionType == WinningOptionType.BONUS) {
			rewardEvent.setBonusPointsWon(winning.getAmount().longValue());
		} else if (winningOptionType == WinningOptionType.MONEY) {
			rewardEvent.setMoneyWon(winning.getAmount());
		}

		tracker.addEvent(clientInfo, rewardEvent);
	}

}
