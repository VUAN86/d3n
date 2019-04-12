package de.ascendro.f4m.server.event.log;

import javax.inject.Inject;

import org.apache.commons.lang3.Validate;

import com.aerospike.client.Bin;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.event.log.util.EventLogPrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.json.model.EventLog;

public class EventLogAerospikeDaoImpl extends AerospikeDaoImpl<EventLogPrimaryKeyUtil> implements EventLogAerospikeDao {

	@Inject
	public EventLogAerospikeDaoImpl(Config config, EventLogPrimaryKeyUtil primaryKeyUtil, JsonUtil jsonUtil,
			AerospikeClientProvider aerospikeClientProvider) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	public String createEventLog(EventLog eventLog) {
		Validate.notNull(eventLog);
		
		final String eventLogId = primaryKeyUtil.generateId();
		eventLog.setId(eventLogId);

		final String eventLogKey = primaryKeyUtil.createPrimaryKey(eventLogId);

		createRecord(getSet(), eventLogKey, 
				getJsonBin(BLOB_BIN_NAME, eventLog.getAsString()), 
				new Bin(EVENT_LOG_TYPE_BIN_NAME, eventLog.getEventLogType() == null ? null : eventLog.getEventLogType().getValue()),
				new Bin(CATEGORY_BIN_NAME, eventLog.getCategory()),
				new Bin(USER_ID_BIN_NAME, eventLog.getUserId()),
				new Bin(GAME_INSTANCE_ID_BIN_NAME, eventLog.getGameInstanceId()),
				new Bin(QUESTION_INDEX_BIN_NAME, eventLog.getQuestionIndex()));

		return eventLogId;
	}

	protected String getSet() {
		return config.getProperty(AerospikeConfigImpl.EVENT_LOG_SET);
	}

}
