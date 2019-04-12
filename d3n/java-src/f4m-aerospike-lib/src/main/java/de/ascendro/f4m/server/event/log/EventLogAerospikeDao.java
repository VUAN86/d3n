package de.ascendro.f4m.server.event.log;

import de.ascendro.f4m.service.json.model.EventLog;

public interface EventLogAerospikeDao {

	public static final String BLOB_BIN_NAME = "value";
	public static final String EVENT_LOG_TYPE_BIN_NAME = "eventLogType";
	public static final String CATEGORY_BIN_NAME = "category";
	public static final String USER_ID_BIN_NAME = "userId";
	public static final String GAME_INSTANCE_ID_BIN_NAME = "gameInstanceId";
	public static final String QUESTION_INDEX_BIN_NAME = "questionIndex";

	/**
	 * Store an event log entry
	 * 
	 * @param eventLog
	 *            {@link EventLog}
	 * 
	 * @return ID of the created event log
	 */
	public String createEventLog(EventLog eventLog);

}
