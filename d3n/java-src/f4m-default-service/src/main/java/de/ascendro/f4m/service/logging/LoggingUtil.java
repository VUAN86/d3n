package de.ascendro.f4m.service.logging;

import javax.inject.Inject;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.google.gson.Gson;

import de.ascendro.f4m.service.config.F4MConfig;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.session.SessionWrapper;

public class LoggingUtil {

	private static final Logger PROTOCOL_LOGGER = LoggerFactory.getLogger("de.ascendro.f4m.protocol");
	private static final Logger SERVICE_STATISTICS_LOGGER = LoggerFactory.getLogger("de.ascendro.f4m.serviceStatistics");
	private static final Logger EXECUTION_STATISTICS_LOGGER = LoggerFactory.getLogger("de.ascendro.f4m.executionStatistics");
	
	private static final Marker PROTOCOL = MarkerFactory.getMarker("PROTOCOL");
	private static final Marker SERVICE_STATISTICS = MarkerFactory.getMarker("SERVICE_STATISTICS");
	private static final Marker EXECUTION_STATISTICS = MarkerFactory.getMarker("EXECUTION_STATISTICS");

	private final F4MConfigImpl config;
	private Gson gson = new GsonProvider().get();

	@Inject
	public LoggingUtil(F4MConfigImpl config) {
		this.config = config;
	}

	public void logProtocol(LoggedMessageType type, LoggedMessageAction action, String message) {
		if (PROTOCOL_LOGGER.isInfoEnabled() && isProtocolLoggingEnabled()) {
			try (final CloseableThreadContext.Instance logContext = 
					CloseableThreadContext.put(LoggingThreadContextKey.PROTOCOL_LOG_TYPE.getValue(), type.name())) {
				internalLogProtocol(logContext, action, message);
			}
		}
	}
	
	public void logProtocolMessage(LoggedMessageType type, LoggedMessageAction action, String message,  
			String protocolMessage, SessionWrapper sessionWrapper, boolean isHeartbeat) {
		if (PROTOCOL_LOGGER.isInfoEnabled() && isProtocolLoggingEnabled() && (! isHeartbeat || isHeartbeatLogged())) {
			try (final CloseableThreadContext.Instance logContext = 
					CloseableThreadContext.put(LoggingThreadContextKey.PROTOCOL_LOG_TYPE.getValue(), type.name())) {
				if (protocolMessage != null) {
					saveInThreadContext(logContext, LoggingThreadContextKey.PROTOCOL_LOG_MESSAGE, protocolMessage);
				}

				String from;
				String fromServiceName;
				String to;
				String toServiceName;
				if (action == LoggedMessageAction.RECEIVED) {
					from = sessionWrapper.getTarget();
					fromServiceName = sessionWrapper.getConnectedClientServiceName();
					to = sessionWrapper.getSource();
					toServiceName = getServiceName();
				} else {
					from = sessionWrapper.getSource();
					fromServiceName = getServiceName();
					to = sessionWrapper.getTarget();
					toServiceName = sessionWrapper.getConnectedClientServiceName();
				}
				
				saveInThreadContext(logContext, LoggingThreadContextKey.PROTOCOL_LOG_FROM, from);
				saveInThreadContext(logContext, LoggingThreadContextKey.PROTOCOL_LOG_FROM_SERVICE_NAME, fromServiceName);
				saveInThreadContext(logContext, LoggingThreadContextKey.PROTOCOL_LOG_TO, to);
				saveInThreadContext(logContext, LoggingThreadContextKey.PROTOCOL_LOG_TO_SERVICE_NAME, toServiceName);
				saveInThreadContext(logContext, LoggingThreadContextKey.PROTOCOL_LOG_IS_CLIENT, sessionWrapper.isClient());
				internalLogProtocol(logContext, action, message);
			}
		}
	}

	public void logProtocolMQMessage(LoggedMessageType type, LoggedMessageAction action, String message,
			String protocolMessage, boolean isHeartbeat) {
		if (PROTOCOL_LOGGER.isInfoEnabled() && isProtocolLoggingEnabled() && (! isHeartbeat || isHeartbeatLogged())) {
			try (final CloseableThreadContext.Instance logContext =
					CloseableThreadContext.put(LoggingThreadContextKey.PROTOCOL_LOG_TYPE.getValue(), type.name())) {
				if (protocolMessage != null) {
					saveInThreadContext(logContext, LoggingThreadContextKey.PROTOCOL_LOG_MESSAGE, protocolMessage);
				}

//				String from;
//				String fromServiceName;
//				String to;
//				String toServiceName;
//				if (action == LoggedMessageAction.RECEIVED) {
//					from = sessionWrapper.getTarget();
//					fromServiceName = sessionWrapper.getConnectedClientServiceName();
//					to = sessionWrapper.getSource();
//					toServiceName = getServiceName();
//				} else {
//					from = sessionWrapper.getSource();
//					fromServiceName = getServiceName();
//					to = sessionWrapper.getTarget();
//					toServiceName = sessionWrapper.getConnectedClientServiceName();
//				}

//				saveInThreadContext(logContext, LoggingThreadContextKey.PROTOCOL_LOG_FROM, from);
//				saveInThreadContext(logContext, LoggingThreadContextKey.PROTOCOL_LOG_FROM_SERVICE_NAME, fromServiceName);
//				saveInThreadContext(logContext, LoggingThreadContextKey.PROTOCOL_LOG_TO, to);
//				saveInThreadContext(logContext, LoggingThreadContextKey.PROTOCOL_LOG_TO_SERVICE_NAME, toServiceName);
//				saveInThreadContext(logContext, LoggingThreadContextKey.PROTOCOL_LOG_IS_CLIENT, sessionWrapper.isClient());
				internalLogProtocol(logContext, action, message);
			}
		}
	}
	
	private void internalLogProtocol(CloseableThreadContext.Instance logContext, LoggedMessageAction action, String message) {
		saveInThreadContext(logContext, LoggingThreadContextKey.PROTOCOL_LOG_ACTION, action);
		PROTOCOL_LOGGER.info(PROTOCOL, message);
	}

	public void logServiceStatistics(MonitoringInformation statistics) {
		try (final CloseableThreadContext.Instance logContext = 
				CloseableThreadContext.put(LoggingThreadContextKey.SERVICE_STATISTICS.getValue(), gson.toJson(statistics))) {
			SERVICE_STATISTICS_LOGGER.info(SERVICE_STATISTICS, "Service statistics gathered");
		}
	}
	
	public void logExecutionStatistics(String message, String messageName, String originalRequestMessageName, long executionTime) {
		try (final CloseableThreadContext.Instance logContext = CloseableThreadContext
				.put(LoggingThreadContextKey.EXECUTION_LOG_TIME.getValue(), String.valueOf(executionTime))
				.put(LoggingThreadContextKey.EXECUTION_LOG_MESSAGE_NAME.getValue(), messageName)
				.put(LoggingThreadContextKey.EXECUTION_LOG_ORIGINAL_REQUEST_MESSAGE_NAME.getValue(), originalRequestMessageName)) {
			EXECUTION_STATISTICS_LOGGER.info(EXECUTION_STATISTICS, message);
		}
	}
	
	public void saveBasicInformationInThreadContext() {
		saveInThreadContext(LoggingThreadContextKey.SERVICE_NAME, getServiceName());
		saveInThreadContext(LoggingThreadContextKey.SERVICE_ENDPOINT, getServiceEndpointIdentifier());
	}

	private String getServiceName() {
		return config.getProperty(F4MConfig.SERVICE_NAME);
	}
	
	private String getServiceEndpointIdentifier() {
		return config.getServiceEndpointIdentifier();
	}

	private void saveInThreadContext(LoggingThreadContextKey key, Object value) {
		ThreadContext.put(key.getValue(), value == null ? null : value.toString());
	}
	
	private void saveInThreadContext(CloseableThreadContext.Instance logContext, LoggingThreadContextKey key, Object value) {
		logContext.put(key.getValue(), value == null ? null : value.toString());
	}
	
	private boolean isProtocolLoggingEnabled() {
		return config.getPropertyAsBoolean(F4MConfig.F4M_PROTOCOL_LOGGING) == Boolean.TRUE;
	}

	private boolean isHeartbeatLogged() {
		return config.getPropertyAsBoolean(F4MConfig.F4M_HEARTBEAT_LOGGING) == Boolean.TRUE;
	}

}
