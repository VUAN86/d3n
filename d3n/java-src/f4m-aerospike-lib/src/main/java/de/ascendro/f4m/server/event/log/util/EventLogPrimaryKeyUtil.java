package de.ascendro.f4m.server.event.log.util;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;

public class EventLogPrimaryKeyUtil extends PrimaryKeyUtil<String> {

	public static final String AEROSPIKE_KEY_PREFIX_EVENT_LOG = "eventLog";

	@Inject
	public EventLogPrimaryKeyUtil(Config config) {
		super(config);
	}

	@Override
	protected String getServiceName() {
		return AEROSPIKE_KEY_PREFIX_EVENT_LOG;
	}

}
