package de.ascendro.f4m.service.profile.model.resync;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ResyncResponse implements JsonMessageContent {
	
	private int queuedForResyncCount;

	public ResyncResponse(int queuedForResyncCount) {
		this.queuedForResyncCount = queuedForResyncCount;
	}

	public int getQueuedForResyncCount() {
		return queuedForResyncCount;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("queuedForResyncCount=").append(queuedForResyncCount);
		builder.append("]");
		return builder.toString();
	}

}
