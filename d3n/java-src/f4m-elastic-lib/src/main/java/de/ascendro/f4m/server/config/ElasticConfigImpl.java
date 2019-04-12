package de.ascendro.f4m.server.config;

import com.google.common.io.Resources;

import de.ascendro.f4m.server.friend.CommonBuddyElasticDao;
import de.ascendro.f4m.server.profile.CommonProfileElasticDao;
import de.ascendro.f4m.service.config.F4MConfig;

public class ElasticConfigImpl extends F4MConfig {

	public static final String ELASTIC_HEADER_VALUE_WAIT_FOR = "wait_for";
	
	public static final String ELASTIC_HEADER_REFRESH = "elastic.header.refresh";
	public static final String ELASTIC_SERVER_HOSTS = "elastic.server.hosts";

	public static final String ELASTIC_INDEX_PROFILE = "elastic.index.profile";
	public static final String ELASTIC_TYPE_PROFILE = "elastic.type.profile";
	public static final String ELASTIC_TYPE_BUDDY = "elastic.type.buddy";

	public static final String ELASTIC_MAPPING_INDEX_PROFILE = "elastic.mapping.index.profile";
	public static final String ELASTIC_MAPPING_TYPE_PROFILE = "elastic.mapping.type.profile";
	public static final String ELASTIC_MAPPING_TYPE_BUDDY = "elastic.mapping.type.buddy";
	
	public ElasticConfigImpl() {
		setProperty(ELASTIC_SERVER_HOSTS, "http://localhost:9200");

		setProperty(ELASTIC_INDEX_PROFILE, "profile");
		
		setProperty(ELASTIC_TYPE_PROFILE, "profile");

		setProperty(ELASTIC_TYPE_BUDDY, "buddy");

		setProperty(ELASTIC_MAPPING_INDEX_PROFILE, Resources.getResource(CommonProfileElasticDao.class, "ProfileIndexESMapping.json"));
		setProperty(ELASTIC_MAPPING_TYPE_PROFILE, Resources.getResource(CommonProfileElasticDao.class, "ProfileESMapping.json"));
		setProperty(ELASTIC_MAPPING_TYPE_BUDDY, Resources.getResource(CommonBuddyElasticDao.class, "BuddyESMapping.json"));
	}

}
