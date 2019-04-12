package de.ascendro.f4m.service.friend.dao;

import static de.ascendro.f4m.server.util.ElasticUtil.buildPath;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.elastic.ElasticClient;
import de.ascendro.f4m.server.elastic.ElasticResponse;
import de.ascendro.f4m.server.elastic.query.BoolQuery;
import de.ascendro.f4m.server.elastic.query.Function;
import de.ascendro.f4m.server.elastic.query.Query;
import de.ascendro.f4m.server.elastic.query.RootQuery;
import de.ascendro.f4m.server.elastic.query.Sort;
import de.ascendro.f4m.server.elastic.query.SortDirection;
import de.ascendro.f4m.server.util.ElasticUtil;
import de.ascendro.f4m.service.friend.config.FriendManagerConfig;
import de.ascendro.f4m.service.friend.model.Buddy;
import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyListOrderType;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileUser;

public class BuddyElasticDaoImpl implements BuddyElasticDao {
	private static final Logger LOGGER = LoggerFactory.getLogger(BuddyElasticDaoImpl.class);

	private static final String RAW_SUFFIX = ".raw";
	private static final String PROPERTY_SCORE = "_score";

	private static final String PROPERTY_BUDDY_OWNER_ID_RAW = Buddy.PROPERTY_OWNER_ID + RAW_SUFFIX;
	private static final String PROPERTY_BUDDY_USER_ID_RAW = Buddy.PROPERTY_USER_ID + RAW_SUFFIX;
	private static final String PROPERTY_TENANT_IDS_RAW = Buddy.PROPERTY_TENANT_IDS + RAW_SUFFIX;
	private static final String PROPERTY_APPLICATIONS_RAW = Profile.APPLICATIONS_PROPERTY_NAME + RAW_SUFFIX;

	private static final String PROPERTY_NAME_SEARCH_NAME = "searchName";
	private static final String PROPERTY_SEARCH_NAME = buildPath(Profile.PERSON_PROPERTY, PROPERTY_NAME_SEARCH_NAME);

	private static final String PROPERTY_BUDDY_SEARCH_NAME = "buddySearchName";
	private static final String PROPERTY_BUDDY_SEARCH_NAME_RAW = PROPERTY_BUDDY_SEARCH_NAME + RAW_SUFFIX;

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

	private static final String PROPERTY_RELATION_TYPES_RAW = Buddy.PROPERTY_RELATION_TYPES + RAW_SUFFIX;
	
	private final ElasticClient elasticClient;
	private ElasticUtil elasticUtil;
	private final FriendManagerConfig config;
	private boolean indexInitialized;
	
	@Inject
	public BuddyElasticDaoImpl(ElasticClient elasticClient, FriendManagerConfig config, ElasticUtil elasticUtil) {
		this.elasticClient = elasticClient;
		this.config = config;
		this.elasticUtil = elasticUtil;
	}

	@Override
	public void createOrUpdate(String searchName, Buddy buddy) {
		initIndex();
		JsonObject buddyJson = elasticUtil.copyJson(buddy.getJsonObject(), JsonObject.class);
		
		// Unfortunately elastic does not allow to sort by parent field, and AWS has disabled scripting, so best solution 
		// for searching by name is keeping a (possibly outdated) searchName field. Will update the field on any operation on buddy.
		if (searchName != null) {
			buddyJson.addProperty(PROPERTY_BUDDY_SEARCH_NAME, searchName);
		} else {
			buddyJson.remove(PROPERTY_BUDDY_SEARCH_NAME);
		}
		elasticClient.createOrUpdate(getIndex(), getType(), buddy.getId(), buddyJson, buddy.getUserId());
	}
	
	@Override
	public void deleteBuddy(String ownerId, String buddyId, boolean wait, boolean silent) {
		initIndex();
		elasticClient.delete(getIndex(), getType(), Buddy.getBuddyId(ownerId, buddyId), buddyId, wait, silent);
	}

	@Override
	public ListResult<Buddy> getBuddyList(String ownerId, String appId, String tenantId, String searchTerm, String email,
			BuddyRelationType[] includedRelationTypes, BuddyRelationType[] excludedRelationTypes, String[] excludedUserIds,
			Boolean favorite, BuddyListOrderType orderType, int limit, long offset) {
		initIndex();

		BoolQuery query = getBuddyListQuery(ownerId, appId, tenantId, searchTerm, email, includedRelationTypes, excludedRelationTypes, excludedUserIds, favorite);
		Sort sort = getBuddyListSort(orderType);
		ElasticResponse response = elasticClient.query(getIndex(), getType(),
				RootQuery.query(query.build()).sort(sort).from(offset).size(limit));
		return new ListResult<>(limit, offset, response.getTotal(), 
				response.getResults().stream().map(Buddy::new).collect(Collectors.toList()));
	}

	@Override
	public Pair<Long, Long> getRank(String userId, String appId, String tenantId, Double handicap) {
		initIndex();

		BoolQuery query = getBuddyListQuery(userId, appId, tenantId, null, null, new BuddyRelationType[] { BuddyRelationType.BUDDY },
				new BuddyRelationType[] { BuddyRelationType.BLOCKED_BY, BuddyRelationType.BLOCKED }, new String[] { userId }, null);
		query.addMust(Query.bool()
				.addShould(Query.hasParent("profile").query(
						handicap == null ? Query.exists(Profile.HANDICAP_PROPERTY) : Query.range(Profile.HANDICAP_PROPERTY).gt(handicap).build()))
				.addShould(Query.bool()
						.addMust(Query.hasParent("profile").query(
								handicap == null ? Query.not(Query.exists(Profile.HANDICAP_PROPERTY)) : Query.term(Profile.HANDICAP_PROPERTY, handicap)))
						.addMust(Query.range(PROPERTY_BUDDY_USER_ID_RAW).lt(userId).build())
						.build())
				.build());
		Sort sort = getBuddyListSort(BuddyListOrderType.NONE);
		ElasticResponse rank = elasticClient.query(getIndex(), getType(), RootQuery.query(query.build()).sort(sort).size(0));

		ListResult<Buddy> totalBuddies = getBuddyList(userId, appId, tenantId, null, null, new BuddyRelationType[] { BuddyRelationType.BUDDY },
				new BuddyRelationType[] { BuddyRelationType.BLOCKED_BY, BuddyRelationType.BLOCKED }, new String[] { userId }, null, BuddyListOrderType.NONE, 0, 0);
		return Pair.of(rank.getTotal() + 1, totalBuddies.getTotal() + 1);
	}

	private BoolQuery getBuddyListQuery(String ownerId, String appId, String tenantId, String searchTerm, String email,
			BuddyRelationType[] includedRelationTypes, BuddyRelationType[] excludedRelationTypes, String[] excludedUserIds, Boolean favorite) {
		// Build profile query
		BoolQuery profileQuery = Query.bool()
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
			profileQuery.addMust(Query.term(PROPERTY_APPLICATIONS_RAW, appId));
		}
		// Build main query
		BoolQuery query = Query.bool()
				.addMust(Query.term(PROPERTY_BUDDY_OWNER_ID_RAW, ownerId))
				.addMust(Query.term(PROPERTY_TENANT_IDS_RAW, tenantId));
			 	String regex = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")(@|%40)(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
		if (StringUtils.isNotBlank(searchTerm) && searchTerm.matches(regex)) {
			query.addMust(Query.term(Buddy.PROPERTY_EMAIL, searchTerm));
		} else if (StringUtils.isNotBlank(searchTerm) && searchTerm.matches("(\\W*\\w{3,}\\W*)+")) {
			// Search entered and has at least one word at least 3 chars long - add filter
			profileQuery.addMust(Query.nested(Profile.PERSON_PROPERTY).query(
					Query.bool()
							.addShould(Query.match(PROPERTY_SEARCH_NAME).query(searchTerm).minimumShouldMatch(2).build())
							.addShould(Query.match(PROPERTY_NICKNAME).query(searchTerm).minimumShouldMatch(2).build())
							.build()
			));
		}
		query = query
				.addMust(
						Query
								.hasParent("profile")
								.scoreType("score")
								.query(
										Query
												.functionScore()
												.functions(
														Function
																.fieldValueFactor()
																.missing(0)
																.field(
																		Profile.HANDICAP_PROPERTY
																)
																.build()
												)
												.query(
														profileQuery
																.build()
												)
								)
				);
		if (ArrayUtils.isNotEmpty(includedRelationTypes)) {
			query.addMust(Query.terms(PROPERTY_RELATION_TYPES_RAW, 
					Arrays.stream(includedRelationTypes).map(r -> r.name()).toArray()));
		}
		if (ArrayUtils.isNotEmpty(excludedRelationTypes)) {
			query.addMust(Query.not(Query.terms(PROPERTY_RELATION_TYPES_RAW, 
					Arrays.stream(excludedRelationTypes).map(r -> r.name()).toArray())));
		}
		if (ArrayUtils.isNotEmpty(excludedUserIds)) {
			query.addMust(Query.not(Query.terms(PROPERTY_BUDDY_USER_ID_RAW, excludedUserIds)));
		}
        if (favorite != null) {
            query.addMust(Query.term(Buddy.PROPERTY_IS_FAVORITE, favorite));
        }
		return query;
	}

	private Sort getBuddyListSort(BuddyListOrderType orderType) {
		switch (orderType == null ? BuddyListOrderType.NONE : orderType) {
		case HANDICAP:
			return Sort.newSort(PROPERTY_SCORE, SortDirection.desc).sort(PROPERTY_BUDDY_USER_ID_RAW);
		case RECENT:
			return Sort.newSort(Buddy.PROPERTY_LAST_INTERACTION_TIMESTAMP, SortDirection.desc).sort(PROPERTY_BUDDY_USER_ID_RAW);
		case STRENGH:
			return Sort.newSort(Buddy.PROPERTY_INTERACTION_COUNT, SortDirection.desc).sort(PROPERTY_BUDDY_USER_ID_RAW);
		case NONE:
		default:
			return Sort.newSort(Buddy.PROPERTY_IS_FAVORITE).sort(PROPERTY_BUDDY_SEARCH_NAME_RAW).sort(PROPERTY_BUDDY_USER_ID_RAW);
		}
	}

	@Override
	public List<String> getAllOwnerIds(String buddyId) {
		initIndex();
		
		return elasticClient.queryAll(getIndex(), getType(), 
				Query.bool().addMust(Query.term(PROPERTY_BUDDY_USER_ID_RAW, buddyId)).build(), 
				PROPERTY_BUDDY_USER_ID_RAW, r -> r.get(Buddy.PROPERTY_OWNER_ID).getAsString());
	}
	
	private String getIndex() {
		return config.getProperty(ElasticConfigImpl.ELASTIC_INDEX_PROFILE);
	}

	private String getType() {
		return config.getProperty(FriendManagerConfig.ELASTIC_TYPE_BUDDY);
	}
	
	private void initIndex() {
		if (! indexInitialized) {
			synchronized(this) {
				if (! indexInitialized) {
					String index = getIndex();
					if (! elasticClient.indexExists(index)) {
						elasticClient.createIndex(index, config.getProperty(ElasticConfigImpl.ELASTIC_MAPPING_INDEX_PROFILE));
					}
					elasticClient.createMapping(index, config.getProperty(ElasticConfigImpl.ELASTIC_TYPE_PROFILE), config.getProperty(ElasticConfigImpl.ELASTIC_MAPPING_TYPE_PROFILE));
					elasticClient.createMapping(index, getType(), config.getProperty(FriendManagerConfig.ELASTIC_MAPPING_TYPE_BUDDY));
					indexInitialized = true;
				}
			}
		}
	}

}
