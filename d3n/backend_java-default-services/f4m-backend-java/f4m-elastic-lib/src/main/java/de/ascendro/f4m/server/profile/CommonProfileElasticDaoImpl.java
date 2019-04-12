package de.ascendro.f4m.server.profile;

import static de.ascendro.f4m.server.util.ElasticUtil.buildPath;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.JsonObject;
import java.util.*;

import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.elastic.ElasticClient;
import de.ascendro.f4m.server.elastic.ElasticResponse;
import de.ascendro.f4m.server.elastic.query.BoolQuery;
import de.ascendro.f4m.server.elastic.query.Query;
import de.ascendro.f4m.server.elastic.query.RootQuery;
import de.ascendro.f4m.server.elastic.query.Sort;
import de.ascendro.f4m.server.elastic.query.SortDirection;
import de.ascendro.f4m.server.util.ElasticUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonProfileElasticDaoImpl implements CommonProfileElasticDao {
	private static final Logger LOGGER = LoggerFactory.getLogger(CommonProfileElasticDaoImpl.class);

	private static final String RAW_SUFFIX = ".raw";

	private static final String PROPERTY_NAME_SEARCH_NAME = "searchName";
	private static final String PROPERTY_SEARCH_NAME = buildPath(Profile.PERSON_PROPERTY, PROPERTY_NAME_SEARCH_NAME);
	private static final String PROPERTY_SEARCH_NAME_RAW = PROPERTY_SEARCH_NAME + RAW_SUFFIX;
	
	private static final String PROPERTY_FIRST_NAME = buildPath(Profile.PERSON_PROPERTY, ProfileUser.PERSON_FIRST_NAME_PROPERTY);
	private static final String PROPERTY_FIRST_NAME_RAW = PROPERTY_FIRST_NAME + RAW_SUFFIX;
	
	private static final String PROPERTY_LAST_NAME = buildPath(Profile.PERSON_PROPERTY, ProfileUser.PERSON_LAST_NAME_PROPERTY);
	private static final String PROPERTY_LAST_NAME_RAW = PROPERTY_LAST_NAME + RAW_SUFFIX;

	private static final String PROPERTY_NICKNAME = buildPath(Profile.PERSON_PROPERTY, ProfileUser.NICKNAME_PROPERTY);
	private static final String PROPERTY_NICKNAME_RAW = PROPERTY_NICKNAME + RAW_SUFFIX;
	
	private static final String PROPERTY_CITY = buildPath(Profile.ADDRESS_PROPERTY, ProfileAddress.ADDRESS_CITY_PROPERTY);
	private static final String PROPERTY_CITY_RAW = PROPERTY_CITY + RAW_SUFFIX;
	
	private static final String PROPERTY_COUNTRY = buildPath(Profile.ADDRESS_PROPERTY, ProfileAddress.ADDRESS_COUNTRY_PROPERTY);
	private static final String PROPERTY_COUNTRY_RAW = PROPERTY_COUNTRY + RAW_SUFFIX;
	
	private static final String PROPERTY_USER_ID_RAW = Profile.USER_ID_PROPERTY + RAW_SUFFIX;
	
	private static final String PROPERTY_APPLICATIONS_RAW = Profile.APPLICATIONS_PROPERTY_NAME + RAW_SUFFIX;

	private final ElasticClient elasticClient;
	private final ElasticUtil elasticUtil;
	private final Config config;
	private boolean indexInitialized;
	
	@Inject
	public CommonProfileElasticDaoImpl(ElasticClient elasticClient, Config config, ElasticUtil elasticUtil) {
		this.elasticClient = elasticClient;
		this.elasticUtil = elasticUtil;
		this.config = config;
	}

	@Override
	public void createOrUpdate(Profile profile) {
		initIndex();
		JsonObject json = elasticUtil.copyJson(profile.getJsonObject(), JsonObject.class);
		String searchName = profile.getSearchName();
		if (searchName != null) {
			json.get(Profile.PERSON_PROPERTY).getAsJsonObject().addProperty(PROPERTY_NAME_SEARCH_NAME, searchName);
		}
		Double handicap = profile.getHandicap();
		if (handicap == null) {
			json.addProperty(Profile.HANDICAP_PROPERTY, 0);
		}
		LOGGER.debug("CommonProfileElasticDaoImpl");
		elasticClient.createOrUpdate(getIndex(), getType(), profile.getUserId(), json);
	}

	@Override
	public ListResult<Profile> searchProfiles(String appId, String searchTerm, String[] includeUserIds, String[] excludeUserIds, 
			PlayerListOrderType orderType, int limit, long offset) {
		initIndex();
		BoolQuery query = getProfileSearchQuery(appId, searchTerm, includeUserIds, excludeUserIds);
		Sort sort = getProfileSearchSort(orderType);
		ElasticResponse response = elasticClient.query(getIndex(), getType(),
				RootQuery.query(query.build()).sort(sort).from(offset).size(limit));
		LOGGER.debug("searchProfiles getIndex {}  getType {} query {} ", getIndex(), getType(), query.build());

		return new ListResult<>(limit, offset, response.getTotal(), 
				response.getResults().stream().map(Profile::new).collect(Collectors.toList()));
	}

	@Override
	public List<Profile> searchProfilesWithEmail(String email) {
		initIndex();
		BoolQuery query = getProfileSearchQueryByEmail(email);
		
		ElasticResponse response = elasticClient.query(getIndex(), getType(),
				RootQuery.query(query.build()).from(0).size(1));
		return response.getResults().stream().map(Profile::new).collect(Collectors.toList());
	}

	@Override
	public Pair<Long, Long> getRank(String appId, String userId, Double handicap) {
		initIndex();

		BoolQuery query = getProfileSearchQuery(appId, null, null, userId == null ? null : new String[] { userId });
		if (StringUtils.isNotBlank(userId)) {
			query.addMust(Query.bool()
					.addShould(handicap == null ? Query.exists(Profile.HANDICAP_PROPERTY) : Query.range(Profile.HANDICAP_PROPERTY).gt(handicap).build()) // If no handicap => any handicap is above, otherwise compare
					.addShould(Query.bool()
							.addMust(handicap == null ? Query.not(Query.exists(Profile.HANDICAP_PROPERTY)) : Query.term(Profile.HANDICAP_PROPERTY, handicap))
							.addMust(Query.range(PROPERTY_USER_ID_RAW).lt(userId).build())
							.build())
					.build());
		}
		Sort sort = getProfileSearchSort(PlayerListOrderType.NONE);
		ElasticResponse rank = elasticClient.query(getIndex(), getType(), RootQuery.query(query.build()).sort(sort).size(0));
		
		ListResult<Profile> totalProfiles = searchProfiles(appId, null, null, null, PlayerListOrderType.NONE, 0, 0);
		return Pair.of(rank.getTotal() + 1, 
				totalProfiles.getTotal() <= rank.getTotal() ? rank.getTotal() + 1 : totalProfiles.getTotal());
	}

	private BoolQuery getProfileSearchQuery(String appId, String searchTerm, String[] includeUserIds, String[] excludeUserIds) {
		BoolQuery query = Query.bool()
				.addMust(Query.nested(Profile.PERSON_PROPERTY).query(Query.exists(PROPERTY_FIRST_NAME_RAW)))
				.addMust(Query.nested(Profile.PERSON_PROPERTY).query(Query.not(Query.term(PROPERTY_FIRST_NAME_RAW, ""))))
				.addMust(Query.nested(Profile.PERSON_PROPERTY).query(Query.exists(PROPERTY_LAST_NAME_RAW)))
				.addMust(Query.nested(Profile.PERSON_PROPERTY).query(Query.not(Query.term(PROPERTY_LAST_NAME_RAW, ""))))
				.addMust(Query.nested(Profile.PERSON_PROPERTY).query(Query.exists(PROPERTY_NICKNAME_RAW)))
				.addMust(Query.nested(Profile.PERSON_PROPERTY).query(Query.not(Query.term(PROPERTY_NICKNAME_RAW, ""))))
				.addMust(Query.nested(Profile.ADDRESS_PROPERTY).query(Query.exists(PROPERTY_CITY_RAW)))
				.addMust(Query.nested(Profile.ADDRESS_PROPERTY).query(Query.not(Query.term(PROPERTY_CITY_RAW, ""))))
				.addMust(Query.nested(Profile.ADDRESS_PROPERTY).query(Query.exists(PROPERTY_COUNTRY_RAW)))
				.addMust(Query.nested(Profile.ADDRESS_PROPERTY).query(Query.not(Query.term(PROPERTY_COUNTRY_RAW, ""))));
		if (StringUtils.isNotBlank(appId)) {
			query.addMust(Query.term(PROPERTY_APPLICATIONS_RAW, appId));
		}
		if (StringUtils.isNotBlank(searchTerm) && searchTerm.matches("(\\W*\\w{3,}\\W*)+")) {
			// Search entered and has at least one word at least 3 chars long - add filter
			query.addMust(Query.nested(Profile.PERSON_PROPERTY).query(
				Query.bool()
					.addShould(Query.match(PROPERTY_SEARCH_NAME).query(searchTerm).minimumShouldMatch(2).build())
					.addShould(Query.match(PROPERTY_NICKNAME).query(searchTerm).minimumShouldMatch(2).build())
					.build()
			));
		}
		if (ArrayUtils.isNotEmpty(includeUserIds)) {
			query.addMust(Query.terms(PROPERTY_USER_ID_RAW, includeUserIds));
		}
		if (ArrayUtils.isNotEmpty(excludeUserIds)) {
			query.addMust(Query.not(Query.terms(PROPERTY_USER_ID_RAW, excludeUserIds)));
		}
		return query;
	}

	private BoolQuery getProfileSearchQueryByEmail(String email) {
		BoolQuery query = Query.bool()
				.addMust(Query.nested(Profile.PERSON_PROPERTY).query(Query.exists(PROPERTY_FIRST_NAME_RAW)))
				.addMust(Query.nested(Profile.PERSON_PROPERTY).query(Query.not(Query.term(PROPERTY_FIRST_NAME_RAW, ""))))
				.addMust(Query.nested(Profile.PERSON_PROPERTY).query(Query.exists(PROPERTY_LAST_NAME_RAW)))
				.addMust(Query.nested(Profile.PERSON_PROPERTY).query(Query.not(Query.term(PROPERTY_LAST_NAME_RAW, ""))))
				.addMust(Query.nested(Profile.PERSON_PROPERTY).query(Query.exists(PROPERTY_NICKNAME_RAW)))
				.addMust(Query.nested(Profile.PERSON_PROPERTY).query(Query.not(Query.term(PROPERTY_NICKNAME_RAW, ""))))
				.addMust(Query.nested(Profile.ADDRESS_PROPERTY).query(Query.exists(PROPERTY_CITY_RAW)))
				.addMust(Query.nested(Profile.ADDRESS_PROPERTY).query(Query.not(Query.term(PROPERTY_CITY_RAW, ""))))
				.addMust(Query.nested(Profile.ADDRESS_PROPERTY).query(Query.exists(PROPERTY_COUNTRY_RAW)))
				.addMust(Query.nested(Profile.ADDRESS_PROPERTY).query(Query.not(Query.term(PROPERTY_COUNTRY_RAW, ""))));
				query.addMust(Query.term("emails.email", email));
		return query;
	}

	private Sort getProfileSearchSort(PlayerListOrderType orderType) {
		switch (orderType == null ? PlayerListOrderType.NONE : orderType) {
		case HANDICAP:
			return Sort.newSort(Profile.HANDICAP_PROPERTY, SortDirection.desc).sort(PROPERTY_USER_ID_RAW);
		case NONE:
		default:
			return Sort.newSort(Profile.PERSON_PROPERTY, PROPERTY_SEARCH_NAME_RAW).sort(PROPERTY_USER_ID_RAW);
		}
	}

	private String getIndex() {
		return config.getProperty(ElasticConfigImpl.ELASTIC_INDEX_PROFILE);
	}

	private String getType() {
		return config.getProperty(ElasticConfigImpl.ELASTIC_TYPE_PROFILE);
	}
	
	private void initIndex() {
		if (! indexInitialized) {
			synchronized(this) {
				if (! indexInitialized) {
					String index = getIndex();
					if (! elasticClient.indexExists(index)) {
						elasticClient.createIndex(index, config.getProperty(ElasticConfigImpl.ELASTIC_MAPPING_INDEX_PROFILE));
					}
					elasticClient.createMapping(index, getType(), config.getProperty(ElasticConfigImpl.ELASTIC_MAPPING_TYPE_PROFILE));
					indexInitialized = true;
				}
			}
		}
	}

}
