package de.ascendro.f4m.service.event.model.info;

import com.google.gson.annotations.SerializedName;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class InfoResponse implements JsonMessageContent {

	@SerializedName("jms-port")
	private int jmsPort;

	public int getJmsPort() {
		return jmsPort;
	}

	public void setJmsPort(int jmsPort) {
		this.jmsPort = jmsPort;
	}

}
