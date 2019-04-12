package de.ascendro.f4m.service.util;

import de.ascendro.f4m.service.registry.model.monitor.MonitoringConnectionStatus;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringDbConnectionInfo;

public class MonitoringDbConnectionInfoBuilder {
	private MonitoringDbConnectionInfo info;

	public MonitoringDbConnectionInfoBuilder(MonitoringDbConnectionInfo info) {
		this.info = info;
	}
	
	public MonitoringDbConnectionInfoBuilder mysql(MonitoringConnectionStatus mysql) {
		this.info.setMysql(mysql);
		return this;
	}

	public MonitoringDbConnectionInfoBuilder aerospike(MonitoringConnectionStatus aerospike) {
		this.info.setAerospike(aerospike);
		return this;
	}

	public MonitoringDbConnectionInfoBuilder elastic(MonitoringConnectionStatus elastic) {
		this.info.setElastic(elastic);
		return this;
	}

	public MonitoringDbConnectionInfoBuilder spark(MonitoringConnectionStatus spark) {
		this.info.setSpark(spark);
		return this;
	}

	public MonitoringDbConnectionInfo build() {
		return info;
	}
}
