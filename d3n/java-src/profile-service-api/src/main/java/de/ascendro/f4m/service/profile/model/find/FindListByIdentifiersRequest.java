package de.ascendro.f4m.service.profile.model.find;

import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class FindListByIdentifiersRequest implements JsonMessageContent {

	private List<FindListParameter> identifiers;

	public FindListByIdentifiersRequest() {
		// empty constructor
	}

	public List<FindListParameter> getIdentifiers() {
		return identifiers;
	}

	public void setIdentifiers(List<FindListParameter> identifiers) {
		this.identifiers = identifiers;
	}

}
