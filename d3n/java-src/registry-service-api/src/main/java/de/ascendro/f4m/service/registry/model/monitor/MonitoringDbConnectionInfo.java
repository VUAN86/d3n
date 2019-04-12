package de.ascendro.f4m.service.registry.model.monitor;

public class MonitoringDbConnectionInfo {
	private MonitoringConnectionStatus mysql;
	private MonitoringConnectionStatus aerospike;
	private MonitoringConnectionStatus elastic;
	private MonitoringConnectionStatus spark;

	public MonitoringConnectionStatus getMysql() {
		return mysql;
	}

	public void setMysql(MonitoringConnectionStatus mysql) {
		this.mysql = mysql;
	}

	public MonitoringConnectionStatus getAerospike() {
		return aerospike;
	}

	public void setAerospike(MonitoringConnectionStatus aerospike) {
		this.aerospike = aerospike;
	}

	public MonitoringConnectionStatus getElastic() {
		return elastic;
	}

	public void setElastic(MonitoringConnectionStatus elastic) {
		this.elastic = elastic;
	}

	public MonitoringConnectionStatus getSpark() {
		return spark;
	}

	public void setSpark(MonitoringConnectionStatus spark) {
		this.spark = spark;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MonitoringDbConnectionInfo [mysql=");
		builder.append(mysql);
		builder.append(", aerospike=");
		builder.append(aerospike);
		builder.append(", elastic=");
		builder.append(elastic);
		builder.append(", spark=");
		builder.append(spark);
		builder.append("]");
		return builder.toString();
	}
}
