package de.ascendro.f4m.service.friend.model.api.contact;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ContactImportResponseBase implements JsonMessageContent {
	
	private int importedContactCount;

	public ContactImportResponseBase(int importedContactCount) {
		this.importedContactCount = importedContactCount;
	}

	public int getImportedContactCount() {
		return importedContactCount;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("importedContactCount=").append(importedContactCount);
		builder.append("]");
		return builder.toString();
	}

}
