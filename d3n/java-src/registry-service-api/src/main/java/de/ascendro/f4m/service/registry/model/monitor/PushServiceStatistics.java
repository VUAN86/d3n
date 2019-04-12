package de.ascendro.f4m.service.registry.model.monitor;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Message to be sent by all services to service registry for gathering statistics from all services.
 */
public class PushServiceStatistics implements JsonMessageContent {
	private ServiceStatistics statistics;

	public PushServiceStatistics(ServiceStatistics statistics) {
		this.statistics = statistics;
	}
	
	public ServiceStatistics getStatistics() {
		return statistics;
	}

	public void setStatistics(ServiceStatistics statistics) {
		this.statistics = statistics;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PushServiceStatistics [statistics=");
		builder.append(statistics);
		builder.append("]");
		return builder.toString();
	}
}