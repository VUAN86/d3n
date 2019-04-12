package de.ascendro.f4m.service.friend.dao;

import static de.ascendro.f4m.server.util.ElasticUtil.buildPath;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.elastic.ElasticClient;
import de.ascendro.f4m.server.elastic.ElasticResponse;
import de.ascendro.f4m.server.elastic.query.BoolQuery;
import de.ascendro.f4m.server.elastic.query.Query;
import de.ascendro.f4m.server.elastic.query.RootQuery;
import de.ascendro.f4m.server.elastic.query.Sort;
import de.ascendro.f4m.server.util.ElasticUtil;
import de.ascendro.f4m.service.friend.config.FriendManagerConfig;
import de.ascendro.f4m.service.friend.model.Contact;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.profile.model.Profile;

public class ContactElasticDaoImpl implements ContactElasticDao {

	private static final String RAW_SUFFIX = ".raw";

	private static final String PROPERTY_CONTACT_ID_RAW = Contact.PROPERTY_CONTACT_ID + RAW_SUFFIX;
	
	private static final String PROPERTY_OWNER_ID_RAW = Contact.PROPERTY_OWNER_ID + RAW_SUFFIX;
	private static final String PROPERTY_USER_ID_RAW = Contact.PROPERTY_USER_ID + RAW_SUFFIX;

	private static final String PROPERTY_TENANT_IDS_RAW = Contact.PROPERTY_TENANT_IDS + RAW_SUFFIX;
	private static final String PROPERTY_APPLICATIONS_RAW = Profile.APPLICATIONS_PROPERTY_NAME + RAW_SUFFIX;

	private static final String PROPERTY_SENT_INVITATION_TEXT_APP_ID_RAW = 
			buildPath(Contact.PROPERTY_SENT_INVITATION_TEXTS, Contact.PROPERTY_APPLICATION_ID) + RAW_SUFFIX;

	private static final String PROPERTY_CONTACT_SEARCH_NAME = "contactSearchName";
	private static final String PROPERTY_CONTACT_SEARCH_NAME_RAW = PROPERTY_CONTACT_SEARCH_NAME + RAW_SUFFIX;
	
	private final ElasticClient elasticClient;
	private final ElasticUtil elasticUtil;
	private final FriendManagerConfig config;
	private boolean indexInitialized;
	
	@Inject
	public ContactElasticDaoImpl(ElasticClient elasticClient, FriendManagerConfig config, ElasticUtil elasticUtil) {
		this.elasticClient = elasticClient;
		this.elasticUtil = elasticUtil;
		this.config = config;
	}

	@Override
	public void createOrUpdate(Contact contact) {
		initIndex();
		JsonObject json = elasticUtil.copyJson(contact.getJsonObject(), JsonObject.class);
		json.addProperty(PROPERTY_CONTACT_SEARCH_NAME, StringUtils.trim(
				StringUtils.join(new String[] {contact.getFirstName(), contact.getLastName()}, " ")));
		
		// Have to delete first, since parent may have changed
		List<String> parents = elasticClient.queryAll(getIndex(), getProfileType(), 
				Query.hasChild(getType()).query(Query.term(PROPERTY_CONTACT_ID_RAW, contact.getContactId())),
				PROPERTY_CONTACT_ID_RAW, r-> r.get(Profile.USER_ID_PROPERTY).getAsString());
		parents.add(null);
		parents.forEach(p -> delete(contact.getContactId(), p, true, true));
		
		// If no parent - use contactId for proper routing to any shard
		elasticClient.createOrUpdate(getIndex(), getType(), contact.getContactId(), json, 
				getParentId(contact.getContactId(), contact.getUserId())); 
	}
	
	@Override
	public void delete(String contactId, String userId, boolean wait, boolean silent) {
		initIndex();
		elasticClient.delete(getIndex(), getType(), contactId, getParentId(contactId, userId), wait, silent);
	}
	
	private String getParentId(String contactId, String userId) {
		// For records not having user, user contact ID as parent ID
		return userId == null ? "_" + contactId : userId;
	}
	
	@Override
	public ListResult<Contact> getContactList(String ownerId, String appId, boolean hasApp, String tenantId, String searchTerm,
			Boolean hasInvitation, Boolean hasProfile, String[] includeUserIds, String[] excludeUserIds, int limit, long offset) {
		initIndex();
		BoolQuery query = Query.bool()
				.addMust(Query.term(PROPERTY_TENANT_IDS_RAW, tenantId));
		if (StringUtils.isNotBlank(ownerId)) {
			query.addMust(Query.term(PROPERTY_OWNER_ID_RAW, ownerId));
		}
		if (hasApp) {
			query.addMust(Query.hasParent("profile").query(Query.term(PROPERTY_APPLICATIONS_RAW, appId)));
		}
		if (StringUtils.isNotBlank(searchTerm) && searchTerm.matches("(\\W*\\w{3,}\\W*)+")) {
			// Search entered and has at least one word at least 3 chars long - add filter
			query.addMust(Query.match(PROPERTY_CONTACT_SEARCH_NAME).query(searchTerm).minimumShouldMatch(2).build());
		}
		if (ArrayUtils.isNotEmpty(excludeUserIds)) {
			query.addMust(Query.not(Query.terms(PROPERTY_USER_ID_RAW, excludeUserIds)));
		}
		if (ArrayUtils.isNotEmpty(includeUserIds)) {
			query.addMust(Query.terms(PROPERTY_USER_ID_RAW, includeUserIds));
		}
		if (hasInvitation != null) {
			if (hasInvitation) {
				query.addMust(Query.term(PROPERTY_SENT_INVITATION_TEXT_APP_ID_RAW, appId));
			} else {
				query.addMust(Query.not(Query.term(PROPERTY_SENT_INVITATION_TEXT_APP_ID_RAW, appId)));
			}
		}
		if (hasProfile != null) {
			if (hasProfile) {
				query.addMust(Query.exists(Contact.PROPERTY_USER_ID));
			} else {
				query.addMust(Query.not(Query.exists(Contact.PROPERTY_USER_ID)));
			}
		}
		
		ElasticResponse response = elasticClient.query(getIndex(), getType(),
				RootQuery.query(query.build())
					.sort(Sort.newSort(PROPERTY_CONTACT_SEARCH_NAME_RAW).sort(PROPERTY_CONTACT_ID_RAW))
					.from(offset).size(limit));
		return new ListResult<>(limit, offset, response.getTotal(), 
				response.getResults().stream().map(Contact::new).collect(Collectors.toList()));
	}
	
	@Override
	public List<Contact> listAllContacts(String ownerId) {
		initIndex();
		return elasticClient.queryAll(getIndex(), getType(), Query.bool().addMust(Query.term(PROPERTY_OWNER_ID_RAW, ownerId)).build(), 
				PROPERTY_CONTACT_ID_RAW, Contact::new);
	}
	
	@Override
	public List<Contact> listContacts(String ownerId, String[] contactIds) {
		initIndex();
		BoolQuery query = Query.bool()
				.addMust(Query.term(PROPERTY_OWNER_ID_RAW, ownerId))
				.addMust(Query.terms(PROPERTY_CONTACT_ID_RAW, contactIds));
		return elasticClient.queryAll(getIndex(), getType(), query.build(), PROPERTY_CONTACT_ID_RAW, Contact::new);
	}
	
	private String getIndex() {
		return config.getProperty(ElasticConfigImpl.ELASTIC_INDEX_PROFILE);
	}

	private String getType() {
		return config.getProperty(FriendManagerConfig.ELASTIC_TYPE_CONTACT);
	}
	
	private String getProfileType() {
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
					elasticClient.createMapping(index, getProfileType(), config.getProperty(ElasticConfigImpl.ELASTIC_MAPPING_TYPE_PROFILE));
					elasticClient.createMapping(index, getType(), config.getProperty(FriendManagerConfig.ELASTIC_MAPPING_TYPE_CONTACT));
					indexInitialized = true;
				}
			}
		}
	}

}
