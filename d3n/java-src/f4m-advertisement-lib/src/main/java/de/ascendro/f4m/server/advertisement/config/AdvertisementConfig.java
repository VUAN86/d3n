package de.ascendro.f4m.server.advertisement.config;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.service.config.F4MConfigImpl;

public class AdvertisementConfig extends F4MConfigImpl {

	public static final String ADVERTISEMENT_PATH_FORMAT = "advertisement.blobKeyFormat";
	public static final String ADVERTISEMENT_PATH_FORMAT_DEFAULT = "provider_%d_advertisement_%d.json";
	public static final String AEROSPIKE_ADVERTISEMENT_SET = "aerospike.set.advertisement";

	public AdvertisementConfig() {
		super(new AerospikeConfigImpl(), new AdvertisementConfigImpl());

		setProperty(ADVERTISEMENT_PATH_FORMAT, ADVERTISEMENT_PATH_FORMAT_DEFAULT);
		setProperty(AEROSPIKE_ADVERTISEMENT_SET, "advertisement");
		loadProperties();
	}
}
