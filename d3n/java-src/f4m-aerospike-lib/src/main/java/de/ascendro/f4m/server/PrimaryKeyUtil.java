package de.ascendro.f4m.server;

import java.util.UUID;

import javax.inject.Inject;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfig;

public class PrimaryKeyUtil<T>{
	public static final char KEY_ITEM_SEPARATOR = ':';
	public static final String ACTIVE_CLEANER = "activeCleaner";

	protected final Config config;

	@Inject
	public PrimaryKeyUtil(Config config) {
		this.config = config;
	}

	/**
	 * Base record primary key based on generated Id (@see generateId).
	 * 
	 * @return primary key
	 */
	public String generatePrimaryKey() {
		return getServiceName() + KEY_ITEM_SEPARATOR + generateId();
	}

	/**
	 * Creates primary key
	 *
	 * @param gameId
	 *            {@link String}
	 * @return primary key as [service_name]:[id]
	 */
	public String createPrimaryKey(T id) {
		return getServiceName() + KEY_ITEM_SEPARATOR + id;
	}

	/**
	 * Creates sub-record key, which references base record
	 * 
	 * @param baseRecordKey
	 *            - base record key (@see generatePrimaryKey)
	 * @param subRecordName
	 *            - sub record name
	 * @return sub-record primary key
	 */
	public String createSubRecordPrimaryKey(String baseRecordKey, String subRecordName) {
		return baseRecordKey + KEY_ITEM_SEPARATOR + subRecordName;
	}

	public String generateId() {
		return createGeneratedId();
	}

	public static String createGeneratedId() {
		return System.currentTimeMillis() + UUID.randomUUID().toString();
	}

	public String parseId(String key) {
		final String id;

		if (key != null) {
			id = key.substring(key.indexOf(KEY_ITEM_SEPARATOR) + 1);
		} else {
			id = null;
		}

		return id;
	}

	protected String getServiceName() {
		return config.getProperty(F4MConfig.SERVICE_NAME);
	}

}
