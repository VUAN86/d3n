package de.ascendro.f4m.server.achievement.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.achievement.config.AchievementConfig;
import de.ascendro.f4m.server.achievement.model.BadgeType;
import de.ascendro.f4m.server.achievement.model.UserProgressES;
import de.ascendro.f4m.server.elastic.ElasticClient;
import de.ascendro.f4m.server.elastic.ElasticResponse;
import de.ascendro.f4m.server.elastic.query.BoolQuery;
import de.ascendro.f4m.server.elastic.query.Query;
import de.ascendro.f4m.server.elastic.query.RootQuery;
import de.ascendro.f4m.server.elastic.query.Sort;
import de.ascendro.f4m.server.elastic.query.SortDirection;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;

public class TopUsersElasticDaoImpl implements TopUsersElasticDao {

    private static final String PROPERTY_NUM_OF_COMMUNITY_BADGES = "numOfCommunityBadges";

	private static final String PROPERTY_NUM_OF_GAME_BADGES = "numOfGameBadges";

	private static final String PROPERTY_USER_ID = "userId";

	private static final String TYPE_SEPARATOR = ":";

    private final ElasticClient elasticClient;
    private final AchievementConfig config;
    private JsonUtil jsonUtil;
    private boolean indexInitialized;
    private Map<String, Boolean> mappingInitialized = new ConcurrentHashMap<>();

    @Inject
    public TopUsersElasticDaoImpl(ElasticClient elasticClient, AchievementConfig config, JsonUtil jsonUtil) {
        this.elasticClient = elasticClient;
        this.config = config;
        this.jsonUtil = jsonUtil;
    }

    @Override
    public List<UserProgressES> getTopUsers(String tenantId, BadgeType badgeType, long offset, int limit) {
		return getTopUsers(tenantId, badgeType, new String[]{}, offset, limit);
    }

	@Override
    public List<UserProgressES> getTopUsers(String tenantId, BadgeType badgeType, String[] userIds, long offset, int limit) {
        initIndex(tenantId);
        
    	Sort sort = getSort(badgeType);
		BoolQuery query = Query.bool();
		if (ArrayUtils.isNotEmpty(userIds)) {
			query.addMust(Query.terms(PROPERTY_USER_ID, userIds));
		}

		List<UserProgressES> result = new ArrayList<>();

		try {
            ElasticResponse response = elasticClient.query(getIndex(), getType(tenantId),
                    RootQuery.query(query.build()).sort(sort).from(offset).size(limit));
            List<UserProgressES> list = response.getResults().stream().map(e -> jsonUtil.fromJson(e.toString(), UserProgressES.class)).collect(Collectors.toList());
            result.addAll(list);
        } catch (F4MFatalErrorException e) {
        }
		return result;
    }


    @Override
    public void createOrUpdate(UserProgressES topUser, String tenantId) {
        initIndex(tenantId);
        
        JsonObject topUserJson = (JsonObject) jsonUtil.toJsonElement(topUser);
        elasticClient.createOrUpdate(getIndex(), getType(tenantId), topUser.getUserId(), topUserJson);
    }

	@Override
	public UserProgressES getUserProgress(String tenantId, String userId) {
		initIndex(tenantId);

        BoolQuery query = Query.bool()
                .addMust(Query.term(PROPERTY_USER_ID, userId));

        ElasticResponse response = elasticClient.query(getIndex(), getType(tenantId), RootQuery.query(query.build()));

        if (response.getResults().isEmpty()) {
        	return null;
        } else {
        	JsonObject jsonObject = response.getResults().get(0);
			return jsonUtil.fromJson(jsonObject.getAsString(), UserProgressES.class);
        }
	}

    private Sort getSort(BadgeType badgeType) {
		switch (badgeType) {
			case GAME :
				return Sort.newSort(PROPERTY_NUM_OF_GAME_BADGES, SortDirection.desc).sort("startedOn", SortDirection.asc);
			case COMMUNITY : 
				return Sort.newSort(PROPERTY_NUM_OF_COMMUNITY_BADGES, SortDirection.desc).sort("startedOn", SortDirection.asc);
			default : 
				throw new IllegalArgumentException("only game or community badge counters are supported.");
		}
	}

    private void initIndex(String tenantId) {
        if (! indexInitialized || !isTypeMappingInitialized(tenantId)) {
            synchronized(this) {
                String index = getIndex();
                if (! indexInitialized) {
                    if (! elasticClient.indexExists(index)) {
                        elasticClient.createIndex(index, config.getProperty(AchievementConfig.ELASTIC_MAPPING_INDEX_ACHIEVEMENT));
                    }
                    indexInitialized = true;
                }
                if (!isTypeMappingInitialized(tenantId)) {
                	elasticClient.createMapping(index, getType(tenantId), config.getProperty(AchievementConfig.ELASTIC_MAPPING_TYPE_ACHIEVEMENT));
                	mappingInitialized.put(tenantId, Boolean.TRUE);
                }
            }
        }
    }

	private boolean isTypeMappingInitialized(String tenantId) {
		return Optional.ofNullable(mappingInitialized.get(tenantId)).orElse(Boolean.FALSE);
	}

    private String getType(String tenantId) {
        return config.getProperty(AchievementConfig.ELASTIC_TYPE_ACHIEVEMENT_PREFIX) + TYPE_SEPARATOR + tenantId;
    }


    private String getIndex() {
        return config.getProperty(AchievementConfig.ELASTIC_INDEX_ACHIEVEMENT);
    }
}
