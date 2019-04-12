package de.ascendro.f4m.service.registry.model.monitor;

import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class InfrastructureStatisticsResponse implements JsonMessageContent {
	private List<ServiceStatistics> statisticsList;
	
	public InfrastructureStatisticsResponse(List<ServiceStatistics> statisticsList) {
		this.statisticsList = statisticsList;
	}

	public List<ServiceStatistics> getStatisticsList() {
		return statisticsList;
	}

	public void setStatisticsList(List<ServiceStatistics> statisticsList) {
		this.statisticsList = statisticsList;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InfrastructureStatisticsResponse [statisticsList=");
		builder.append(statisticsList);
		builder.append("]");
		return builder.toString();
	}
}
