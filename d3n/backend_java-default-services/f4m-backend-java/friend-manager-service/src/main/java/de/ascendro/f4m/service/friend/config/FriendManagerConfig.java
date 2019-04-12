package de.ascendro.f4m.service.friend.config;

import com.google.common.io.Resources;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;

public class FriendManagerConfig extends F4MConfigImpl implements Config {

    public static final String AEROSPIKE_GROUP_SET = "group.aerospike.set";
    public static final String AEROSPIKE_OWNED_GROUP_SET = "ownedGroup.aerospike.set";
    public static final String AEROSPIKE_GROUP_MEMBERSHIP_SET = "groupMembership.aerospike.set";
    public static final String AEROSPIKE_CONTACT_SET = "contact.aerospike.set";
    public static final String AEROSPIKE_BUDDY_SET = "buddy.aerospike.set";

    public static final String ELASTIC_TYPE_CONTACT = "elastic.type.contact";
    public static final String ELASTIC_TYPE_BUDDY = "elastic.type.buddy";
    
    public static final String ELASTIC_MAPPING_TYPE_CONTACT = "elastic.mapping.type.contact";
    public static final String ELASTIC_MAPPING_TYPE_BUDDY = "elastic.mapping.type.buddy";

    public FriendManagerConfig() {
		super(new AerospikeConfigImpl(), new ElasticConfigImpl());
		setProperty(AEROSPIKE_GROUP_SET, "group");
		setProperty(AEROSPIKE_OWNED_GROUP_SET, "ownedGroup");
		setProperty(AEROSPIKE_GROUP_MEMBERSHIP_SET, "groupMembership");
		setProperty(AEROSPIKE_CONTACT_SET, "contact");
		setProperty(AEROSPIKE_BUDDY_SET, "buddy");
		setProperty(ELASTIC_TYPE_CONTACT, "contact");
		setProperty(ELASTIC_TYPE_BUDDY, "buddy");
		setProperty(ELASTIC_MAPPING_TYPE_CONTACT, Resources.getResource(getClass(), "ContactESMapping.json"));
		setProperty(ELASTIC_MAPPING_TYPE_BUDDY, Resources.getResource(getClass(), "BuddyESMapping.json"));
		
		loadProperties();
	}
}
