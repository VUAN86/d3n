package de.ascendro.f4m.service.promocode.config;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.service.config.F4MConfigImpl;

public class PromocodeConfig extends F4MConfigImpl {

	public static String AEROSPIKE_PROMOCODE_CAMPAIGN_SET = "promocodeCampaign.aerospike.set";
	public static String AEROSPIKE_PROMOCODE_CAMPAIGN_LIST_SET = "promocodeCampaignList.aerospike.set";
	public static String AEROSPIKE_PROMOCODE_SET = "promocode.aerospike.set";
	public static String PROMOCODE_SYSTEM_REST_URL = "http://quizdom.cbvalidate.de";

	public PromocodeConfig() {
		super(new AerospikeConfigImpl());
		setProperty(AEROSPIKE_PROMOCODE_CAMPAIGN_SET, "promocodeCampaign");
		setProperty(AEROSPIKE_PROMOCODE_CAMPAIGN_LIST_SET, "promocodeCampaignList");
		setProperty(AEROSPIKE_PROMOCODE_SET, "promocode");
		setProperty(PROMOCODE_SYSTEM_REST_URL, "http://quizdom.cbvalidate.de");
		loadProperties();
	}
}
