package de.ascendro.f4m.service.logging;

/**
 * Keys for log4j2 thread context variables.
 */
public enum LoggingThreadContextKey {

	/* General */
	SERVICE_ENDPOINT("serviceEndpoint"),
	SERVICE_NAME("serviceName"),
	
	/* Service statistics */
	SERVICE_STATISTICS("serviceStatistics"),
	
	/* Execution statistics */
	EXECUTION_LOG_TIME("executionTime"),
	EXECUTION_LOG_MESSAGE_NAME("messageName"),
	EXECUTION_LOG_ORIGINAL_REQUEST_MESSAGE_NAME("originalRequestMessageName"),

	/* Protocol */
	PROTOCOL_LOG_TYPE("protocolLogType"),
	PROTOCOL_LOG_ACTION("protocolLogAction"),
	PROTOCOL_LOG_MESSAGE("protocolLogMessage"),
	PROTOCOL_LOG_FROM("protocolLogFrom"),
	PROTOCOL_LOG_FROM_SERVICE_NAME("protocolLogFromServiceName"),
	PROTOCOL_LOG_TO("protocolLogTo"),
	PROTOCOL_LOG_TO_SERVICE_NAME("protocolLogToServiceName"),
	PROTOCOL_LOG_IS_CLIENT("protocolLogIsClient");

	private String value;
	
	private LoggingThreadContextKey(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
}
