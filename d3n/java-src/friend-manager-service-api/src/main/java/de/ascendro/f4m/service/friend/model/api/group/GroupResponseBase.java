package de.ascendro.f4m.service.friend.model.api.group;

import de.ascendro.f4m.service.friend.model.api.ApiGroup;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GroupResponseBase implements JsonMessageContent {

	private ApiGroup group;
	
	public GroupResponseBase(ApiGroup group) {
		this.group = group;
	}

	public ApiGroup getGroup() {
		return group;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("group=").append(group);
		builder.append("]");
		return builder.toString();
	}

}
