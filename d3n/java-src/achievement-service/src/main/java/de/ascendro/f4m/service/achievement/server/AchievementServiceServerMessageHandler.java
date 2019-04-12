package de.ascendro.f4m.service.achievement.server;

import de.ascendro.f4m.service.json.handler.JsonAuthenticationMessageMQHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.achievement.model.Achievement;
import de.ascendro.f4m.server.achievement.model.Badge;
import de.ascendro.f4m.server.achievement.model.BadgeType;
import de.ascendro.f4m.server.achievement.model.UserAchievement;
import de.ascendro.f4m.server.achievement.model.UserBadge;
import de.ascendro.f4m.service.achievement.AchievementMessageTypes;
import de.ascendro.f4m.service.achievement.model.get.AchievementGetRequest;
import de.ascendro.f4m.service.achievement.model.get.AchievementGetResponse;
import de.ascendro.f4m.service.achievement.model.get.AchievementListRequest;
import de.ascendro.f4m.service.achievement.model.get.AchievementListResponse;
import de.ascendro.f4m.service.achievement.model.get.BadgeGetRequest;
import de.ascendro.f4m.service.achievement.model.get.BadgeGetResponse;
import de.ascendro.f4m.service.achievement.model.get.BadgeListRequest;
import de.ascendro.f4m.service.achievement.model.get.BadgeListResponse;
import de.ascendro.f4m.service.achievement.model.get.user.TopUsersRequest;
import de.ascendro.f4m.service.achievement.model.get.user.TopUsersResponse;
import de.ascendro.f4m.service.achievement.model.get.user.UserAchievementGetRequest;
import de.ascendro.f4m.service.achievement.model.get.user.UserAchievementGetResponse;
import de.ascendro.f4m.service.achievement.model.get.user.UserAchievementListRequest;
import de.ascendro.f4m.service.achievement.model.get.user.UserAchievementListResponse;
import de.ascendro.f4m.service.achievement.model.get.user.UserBadgeGetRequest;
import de.ascendro.f4m.service.achievement.model.get.user.UserBadgeGetResponse;
import de.ascendro.f4m.service.achievement.model.get.user.UserBadgeInstanceListRequest;
import de.ascendro.f4m.service.achievement.model.get.user.UserBadgeInstanceListResponse;
import de.ascendro.f4m.service.achievement.model.get.user.UserBadgeListRequest;
import de.ascendro.f4m.service.achievement.model.get.user.UserBadgeListResponse;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.achievement.util.AchievementManager;

public class AchievementServiceServerMessageHandler extends JsonAuthenticationMessageMQHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AchievementServiceServerMessageHandler.class);

	private AchievementManager achievementUtil;

	public AchievementServiceServerMessageHandler(AchievementManager achievementUtil) {
		this.achievementUtil = achievementUtil;
	}

	@Override
	public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> message) {
		final AchievementMessageTypes achievementMessageTypes;

		if ((achievementMessageTypes = message.getType(AchievementMessageTypes.class)) != null) {
			return onAchievementMessage(message.getClientInfo(), message, achievementMessageTypes);
		} else {
			throw new F4MValidationFailedException("Unrecognized message type");
		}
	}

	private JsonMessageContent onAchievementMessage(ClientInfo clientInfo,
			JsonMessage<? extends JsonMessageContent> message, AchievementMessageTypes achievementMessageTypes) {
		switch (achievementMessageTypes) {
		case ACHIEVEMENT_LIST:
			return onAchievementList((AchievementListRequest) message.getContent(), clientInfo);
		case ACHIEVEMENT_GET:
			return onAchievementGet((AchievementGetRequest) message.getContent(), clientInfo);
		case USER_ACHIEVEMENT_LIST:
			return onUserAchievementList((UserAchievementListRequest) message.getContent(), clientInfo);
		case USER_ACHIEVEMENT_GET:
			return onUserAchievementGet((UserAchievementGetRequest) message.getContent(), clientInfo);
		case BADGE_LIST:
			return onBadgeList((BadgeListRequest) message.getContent(), clientInfo);
		case BADGE_GET:
			return onBadgeGet((BadgeGetRequest) message.getContent(), clientInfo);
		case USER_BADGE_LIST:
			return onUserBadgeList((UserBadgeListRequest) message.getContent(), clientInfo);
		case USER_BADGE_GET:
			return onUserBadgeGet((UserBadgeGetRequest) message.getContent(), clientInfo);
		case USER_BADGE_INSTANCE_LIST:
			return onUserBadgeInstanceList((UserBadgeInstanceListRequest) message.getContent(), clientInfo);
		case TOP_USERS_LIST:
			return onTopUsers((TopUsersRequest) message.getContent(), clientInfo);
		default:
			throw new F4MValidationFailedException("Unsupported message type[" + achievementMessageTypes + "]");
		}
	}

	private AchievementListResponse onAchievementList(AchievementListRequest request, ClientInfo clientInfo) {
		request.validateFilterCriteria(AchievementListRequest.MAX_LIST_LIMIT);
		AchievementListResponse response;

		if (request.getLimit() == 0) {
			// empty response
			response = new AchievementListResponse(request.getLimit(), request.getOffset());
		} else {
			LOGGER.debug("Getting achievements for tenant <{}>", clientInfo.getTenantId());
			ListResult<Achievement> items = achievementUtil.getAchievementList(clientInfo.getTenantId(),
					request.getOffset(), request.getLimit());
			response = new AchievementListResponse(request.getLimit(), request.getOffset(), items.getTotal(),
					items.getItems());
		}
		return response;
	}

	private AchievementGetResponse onAchievementGet(AchievementGetRequest request, ClientInfo clientInfo) {
		String achievementId = request.getAchievementId();
		Achievement achievement = achievementUtil.getAchievementById(achievementId);
		if (achievement == null) {
			throw new F4MEntryNotFoundException();
		} else {
			return new AchievementGetResponse(achievement);
		}
	}

	private UserAchievementListResponse onUserAchievementList(UserAchievementListRequest request,
			ClientInfo clientInfo) {
		request.validateFilterCriteria(UserAchievementListRequest.MAX_LIST_LIMIT);

		if (request.getLimit() == 0) {
			// empty response
			return new UserAchievementListResponse(request.getLimit(), request.getOffset());
		} else {
			LOGGER.debug("Getting user achievements for tenant <{}> and user<{}>", clientInfo.getTenantId(), clientInfo.getUserId());
			ListResult<UserAchievement> items = achievementUtil.getUserAchievementList(clientInfo.getTenantId(),
					clientInfo.getUserId(), request.getOffset(), request.getLimit(), request.getStatus());
			return new UserAchievementListResponse(request.getLimit(), request.getOffset(), items.getTotal(),
					items.getItems());
		}
	}

	private UserAchievementGetResponse onUserAchievementGet(UserAchievementGetRequest request, ClientInfo clientInfo) {
		String achievementId = request.getAchievementId();
		String userId = clientInfo.getUserId();
		String tenantId = clientInfo.getTenantId();
		UserAchievement userAchievement = achievementUtil.getUserAchievementbyAchievementId(tenantId, userId,
				achievementId);
		return new UserAchievementGetResponse(userAchievement);
	}

	private BadgeListResponse onBadgeList(BadgeListRequest request, ClientInfo clientInfo) {
		request.validateFilterCriteria(BadgeListRequest.MAX_LIST_LIMIT);
		BadgeListResponse response;

		if (request.getLimit() == 0) {
			// empty response
			response = new BadgeListResponse(request.getLimit(), request.getOffset());
		} else {
			LOGGER.debug("Getting badges for tenant <{}>", clientInfo.getTenantId());
			ListResult<Badge> list = achievementUtil.getBadgeListResponse(clientInfo.getTenantId(), request.getOffset(),
					request.getLimit(), BadgeType.valueOf(request.getBadgeType()));
			response = new BadgeListResponse(request.getLimit(), request.getOffset(), list.getTotal(), list.getItems());
		}
		return response;
	}

	private BadgeGetResponse onBadgeGet(BadgeGetRequest request, ClientInfo clientInfo) {
		String badgeId = request.getBadgeId();
		Badge badge = achievementUtil.getBadgeById(badgeId);
		if (badge == null) {
			throw new F4MEntryNotFoundException();
		} else {
			return new BadgeGetResponse(badge);
		}
	}

	private UserBadgeListResponse onUserBadgeList(UserBadgeListRequest request, ClientInfo clientInfo) {
		request.validateFilterCriteria(UserBadgeListRequest.MAX_LIST_LIMIT);

		if (request.getLimit() == 0) {
			// empty response
			return new UserBadgeListResponse(request.getLimit(), request.getOffset());
		} else {
			LOGGER.debug("Getting user badges for tenant <{}> and user<{}>", clientInfo.getTenantId(), clientInfo.getUserId());
			ListResult<UserBadge> items = achievementUtil.getUserBadgeList(clientInfo.getTenantId(),
					clientInfo.getUserId(), request.getOffset(), request.getLimit(), request.getType());
			return new UserBadgeListResponse(request.getLimit(), request.getOffset(), items.getTotal(),
					items.getItems());
		}
	}

	private UserBadgeGetResponse onUserBadgeGet(UserBadgeGetRequest request, ClientInfo clientInfo) {
		String badgeId = request.getBadgeId();
		UserBadge userBadge = achievementUtil.getUserBadgeByBadgeId(badgeId, clientInfo.getUserId(),
				clientInfo.getTenantId());
		achievementUtil.beautifyProgress(userBadge);
		return new UserBadgeGetResponse(userBadge);
	}

	private UserBadgeInstanceListResponse onUserBadgeInstanceList(UserBadgeInstanceListRequest request,
			ClientInfo clientInfo) {
		request.validateFilterCriteria(UserBadgeInstanceListRequest.MAX_LIST_LIMIT);
		UserBadgeInstanceListResponse response;

		if (request.getLimit() == 0) {
			// empty response
			response = new UserBadgeInstanceListResponse(request.getLimit(), request.getOffset());
		} else {
			LOGGER.debug("Getting user badge instances for tenant <{}> and user<{}> and badge  <{}>", clientInfo.getTenantId(), 
					clientInfo.getUserId(), request.getBadgeId());
			response = achievementUtil.getUserBadgeInstanceListResponse(clientInfo.getTenantId(), request.getOffset(),
					request.getLimit(), request.getBadgeId(), clientInfo.getUserId());
		}
		return response;
	}

	private TopUsersResponse onTopUsers(TopUsersRequest request, ClientInfo clientInfo) {
		TopUsersResponse response;
		if (request.getFriendsOnly()) {
			LOGGER.debug("Getting top friends for tenant <{}> and user<{}>", clientInfo.getTenantId(), clientInfo.getUserId());
			response = achievementUtil.getTopFriends(clientInfo.getTenantId(), clientInfo.getUserId(),
					BadgeType.valueOf(request.getBadgeType()), request.getOffset(), request.getLimit());
		} else {
			LOGGER.debug("Getting top users for tenant <{}>", clientInfo.getTenantId());
			response = achievementUtil.getTopUsers(clientInfo.getTenantId(),
					BadgeType.valueOf(request.getBadgeType()), request.getOffset(), request.getLimit());
		}

		return response;
	}
}
