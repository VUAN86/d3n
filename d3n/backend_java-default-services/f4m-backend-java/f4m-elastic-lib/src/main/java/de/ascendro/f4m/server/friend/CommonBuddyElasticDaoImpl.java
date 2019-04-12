package de.ascendro.f4m.server.friend;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.elastic.ElasticClient;
import de.ascendro.f4m.server.elastic.ElasticResponse;
import de.ascendro.f4m.server.elastic.query.BoolQuery;
import de.ascendro.f4m.server.elastic.query.Query;
import de.ascendro.f4m.server.elastic.query.RootQuery;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.friend.model.Buddy;
import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;
import de.ascendro.f4m.service.profile.model.Profile;

public class CommonBuddyElasticDaoImpl implements CommonBuddyElasticDao {

    private final ElasticClient elasticClient;
    private final Config config;
    private boolean indexInitialized;

    private static final String RAW_SUFFIX = ".raw";
    private static final String PROPERTY_OWNER_ID_RAW = Buddy.PROPERTY_OWNER_ID + RAW_SUFFIX;
    private static final String PROPERTY_USER_ID_RAW = Buddy.PROPERTY_USER_ID + RAW_SUFFIX;
    private static final String PROPERTY_RELATION_TYPES_RAW = Buddy.PROPERTY_RELATION_TYPES + RAW_SUFFIX;
	private static final String PROPERTY_BUDDY_OWNER_ID_RAW = Buddy.PROPERTY_OWNER_ID + RAW_SUFFIX;
	private static final String PROPERTY_BUDDY_USER_ID_RAW = Buddy.PROPERTY_USER_ID + RAW_SUFFIX;
	private static final String PROPERTY_TENANT_IDS_RAW = Buddy.PROPERTY_TENANT_IDS + RAW_SUFFIX;
	private static final String PROPERTY_APPLICATIONS_RAW = Profile.APPLICATIONS_PROPERTY_NAME + RAW_SUFFIX;

    @Inject
    public CommonBuddyElasticDaoImpl(ElasticClient elasticClient, Config config) {
        this.elasticClient = elasticClient;
        this.config = config;
    }

    @Override
    public boolean isMyBuddy(String ownerId, String buddyId) {
        initIndex();

        BoolQuery query = Query.bool()
                .addMust(Query.term(PROPERTY_OWNER_ID_RAW, ownerId));

        query.addMust(Query.term(PROPERTY_USER_ID_RAW, buddyId));

        query.addMust(Query.term(PROPERTY_RELATION_TYPES_RAW, BuddyRelationType.BUDDY.name()));

        ElasticResponse response = elasticClient.query(getIndex(), getType(), RootQuery.query(query.build()));

        return !response.getResults().isEmpty();
    }

	@Override
	public boolean isBlockingMe(String ownerId, String blockerId) {
	    initIndex();

	    BoolQuery query = Query.bool()
                .addMust(Query.term(PROPERTY_OWNER_ID_RAW, ownerId));

        query.addMust(Query.term(PROPERTY_USER_ID_RAW, blockerId));

        query.addMust(Query.term(PROPERTY_RELATION_TYPES_RAW, BuddyRelationType.BLOCKED_BY.name()));

        ElasticResponse response = elasticClient.query(getIndex(), getType(), RootQuery.query(query.build()));

        return !response.getResults().isEmpty();
	}

	@Override
	public List<String> getAllBuddyIds(String ownerId, String appId, String tenantId, BuddyRelationType[] includedRelationTypes, BuddyRelationType[] excludedRelationTypes, String buddyUserId) {
		initIndex();
		
		BoolQuery query = Query.bool()
				.addMust(Query.term(PROPERTY_BUDDY_OWNER_ID_RAW, ownerId));
		
		if (StringUtils.isNotBlank(tenantId)) {
			query.addMust(Query.term(PROPERTY_TENANT_IDS_RAW, tenantId));
		}
		if (StringUtils.isNotBlank(appId)) {
			query.addMust(Query.hasParent("profile").query(Query.term(PROPERTY_APPLICATIONS_RAW, appId)));
		}
		if (ArrayUtils.isNotEmpty(includedRelationTypes)) {
			query.addMust(Query.terms(PROPERTY_RELATION_TYPES_RAW, 
					Arrays.stream(includedRelationTypes).map(r -> r.name()).toArray()));
		}
		if (ArrayUtils.isNotEmpty(excludedRelationTypes)) {
			query.addMust(Query.not(Query.terms(PROPERTY_RELATION_TYPES_RAW, 
					Arrays.stream(excludedRelationTypes).map(r -> r.name()).toArray())));
		}
		if (StringUtils.isNotBlank(buddyUserId)) {
			query.addMust(Query.term(PROPERTY_BUDDY_USER_ID_RAW, buddyUserId));
		}

		return elasticClient.queryAll(getIndex(), getType(), query.build(), PROPERTY_BUDDY_USER_ID_RAW,
				r -> r.get(Buddy.PROPERTY_USER_ID).getAsString());
	}

    private String getIndex() {
        return config.getProperty(ElasticConfigImpl.ELASTIC_INDEX_PROFILE);
    }

    private String getType() {
        return config.getProperty(ElasticConfigImpl.ELASTIC_TYPE_BUDDY);
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
                    elasticClient.createMapping(index, getType(), config.getProperty(ElasticConfigImpl.ELASTIC_MAPPING_TYPE_BUDDY));
                    indexInitialized = true;
                }
            }
        }
    }

}
