package de.ascendro.f4m.service.achievement.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.achievement.dao.AchievementAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.BadgeAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.TopUsersElasticDao;
import de.ascendro.f4m.server.achievement.dao.UserAchievementAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.UserBadgeAerospikeDao;
import de.ascendro.f4m.server.achievement.model.Achievement;
import de.ascendro.f4m.server.achievement.model.Badge;
import de.ascendro.f4m.server.achievement.model.BadgeType;
import de.ascendro.f4m.server.achievement.model.RuleProgress;
import de.ascendro.f4m.server.achievement.model.UserAchievement;
import de.ascendro.f4m.server.achievement.model.UserAchievementStatus;
import de.ascendro.f4m.server.achievement.model.UserBadge;
import de.ascendro.f4m.server.achievement.model.UserProgressES;
import de.ascendro.f4m.server.achievement.model.UserScore;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.achievement.model.get.user.TopUsersResponse;
import de.ascendro.f4m.service.achievement.model.get.user.UserBadgeInstanceListResponse;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.profile.model.Profile;

public class AchievementManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AchievementManager.class);

	private final AchievementAerospikeDao achievementAerospikeDao;
    private final UserAchievementAerospikeDao userAchievementAerospikeDao;

	private final BadgeAerospikeDao badgeAerospikeDao;
    private final UserBadgeAerospikeDao userBadgeAerospikeDao;

	private final TopUsersElasticDao topUsersElasticDao;

	private final CommonBuddyElasticDao commonBuddyElasticDao;
	
	private final CommonProfileAerospikeDao commonProfileAerospikeDao;

	@Inject
	public AchievementManager(AchievementAerospikeDao aerospikeDao, BadgeAerospikeDao badgeAerospikeDao,
			UserAchievementAerospikeDao userAchievementAerospikeDao, UserBadgeAerospikeDao userBadgeAerospikeDao,
			TopUsersElasticDao topUsersElasticDao, CommonBuddyElasticDao commonBuddyElasticDao,
			CommonProfileAerospikeDao commonProfileAerospikeDao) {
        this.achievementAerospikeDao = aerospikeDao;
		this.userAchievementAerospikeDao = userAchievementAerospikeDao;
		this.badgeAerospikeDao = badgeAerospikeDao;
		this.userBadgeAerospikeDao = userBadgeAerospikeDao;
		this.topUsersElasticDao = topUsersElasticDao;
		this.commonBuddyElasticDao = commonBuddyElasticDao;
		this.commonProfileAerospikeDao = commonProfileAerospikeDao;
	}

	public ListResult<Badge> getBadgeListResponse(String tenantId, long offset, int limit, BadgeType badgeType) {
		return badgeAerospikeDao.getBadgeList(tenantId, offset, limit, badgeType);
	}

	public Achievement getAchievementById(String achievementId) {
		return achievementAerospikeDao.getAchievementById(achievementId);
	}

	public Badge getBadgeById(String badgeId) {
		return badgeAerospikeDao.getBadgeById(badgeId);
	}

    public ListResult<Achievement> getAchievementList(String tenantId, long offset, int limit) {
        return achievementAerospikeDao.getAchievementList(tenantId, offset, limit);
    }

	public ListResult<UserAchievement> getUserAchievementList(String tenantId, String userId, long offset, int limit,
                                                              UserAchievementStatus status) {
		return userAchievementAerospikeDao.getUserAchievementList(tenantId, userId, offset, limit, status);
	}

	public UserBadge getUserBadgeByBadgeId(String badgeId, String userId, String tenantId) {
		UserBadge userBadge = userBadgeAerospikeDao.getUserBadgeByBadgeId(badgeId, userId, tenantId);
		if (userBadge == null || userBadge.getTenantId() == null
				|| !userBadge.getTenantId().equals(tenantId)) {
			LOGGER.debug("found user badge <{}> does not match.", userBadge);
			throw new F4MEntryNotFoundException();
		}
		return userBadge;
	}

    public UserBadgeInstanceListResponse getUserBadgeInstanceListResponse(String tenantId, long offset,
                    int limit, String badgeId, String userId) {
        List<UserBadge> userBadges =  userBadgeAerospikeDao.getUserBadgeInstanceList(tenantId, offset, limit, badgeId, userId);
        return new UserBadgeInstanceListResponse(limit, offset, userBadges.size(), userBadges);
    }

	public ListResult<UserBadge> getUserBadgeList(String tenantId, String userId, long offset, int limit,
			BadgeType type) {
		return userBadgeAerospikeDao.getUserBadgeList(tenantId, userId, offset, limit, type);
	}

	public UserAchievement getUserAchievementbyAchievementId(String tenantId, String userId, String achievementId) {
		UserAchievement userAchievement = userAchievementAerospikeDao.getUserAchievementByAchievementId(userId,
				achievementId);
		if (userAchievement == null || userAchievement.getTenantId() == null
				|| !userAchievement.getTenantId().equals(tenantId)) {
			LOGGER.debug("found user achievement <{}> does not match.", userAchievement);
			throw new F4MEntryNotFoundException();
		}
		return userAchievement;
	}

	public TopUsersResponse getTopFriends(String tenantId, String userId, BadgeType type, long offset, int limit) {
		List<String> allBuddyIds = commonBuddyElasticDao.getAllBuddyIds(userId, null, tenantId, new BuddyRelationType[] {}, new BuddyRelationType[] {}, null);
		List<UserProgressES> topUsersES = topUsersElasticDao.getTopUsers(tenantId, type, allBuddyIds.toArray(new String[] {}), offset, limit);
		List<UserScore> topUsers = getUsers(topUsersES);
	    return new TopUsersResponse(limit, offset, topUsers.size(), topUsers);
    }

	public TopUsersResponse getTopUsers(String tenantId, BadgeType type, long offset, int limit) {
		List<UserProgressES> topUsersES = topUsersElasticDao.getTopUsers(tenantId, type, offset, limit);
		List<UserScore> topUsers = getUsers(topUsersES);
	    return new TopUsersResponse(limit, offset, topUsers.size(), topUsers);
	}

	public void beautifyProgress(UserBadge userBadge) {
		userBadge.getRules().forEach(ruleProgress -> beautifyRuleProgress(ruleProgress, userBadge));
	}

	private void beautifyRuleProgress(RuleProgress ruleProgress, UserBadge userBadge) {
    	int wonCount = Optional.ofNullable(userBadge.getWonCount()).orElse(0);
    	int level = Optional.ofNullable(ruleProgress.getLevel()).orElse(0);
		Integer currentProgress = Optional.ofNullable(ruleProgress.getProgress()).orElse(0);
		currentProgress = currentProgress - (wonCount * level);
		currentProgress = currentProgress > level ? level : currentProgress;
		ruleProgress.setProgress(currentProgress);
	}

	private List<UserScore> getUsers(List<UserProgressES> topUserES) {
		List<String> userIds = topUserES.stream().map(user -> user.getUserId()).collect(Collectors.toList());
		List<Profile> profiles = commonProfileAerospikeDao.getProfiles(userIds);
		return getUsers(topUserES, profiles);
	}

	private List<UserScore> getUsers(List<UserProgressES> topUserES, List<Profile> profiles) {
		List<UserScore> top = new ArrayList<>();
		topUserES.stream().forEach(user -> process(user, profiles, top));
		return top;
	}

	private void process(UserProgressES user, List<Profile> profiles, List<UserScore> top) {
		Profile profile = profiles.stream().filter(p -> p.getUserId().equals(user.getUserId())).findFirst().orElseGet(Profile::new);
		UserScore userScore =  new UserScore(user, profile.getFullNameOrNickname(), profile.getImage(), profile.getCreateDate());
		top.add(userScore);
	}
}
