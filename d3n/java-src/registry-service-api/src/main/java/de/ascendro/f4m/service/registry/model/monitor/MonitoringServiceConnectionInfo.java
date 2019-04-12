package de.ascendro.f4m.service.registry.model.monitor;

public class MonitoringServiceConnectionInfo {
	private String serviceName;
	private String address;
	private Integer port;

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MonitoringServiceConnectionInfo [serviceName=");
		builder.append(serviceName);
		builder.append(", address=");
		builder.append(address);
		builder.append(", port=");
		builder.append(port);
		builder.append("]");
		return builder.toString();
	}
}
