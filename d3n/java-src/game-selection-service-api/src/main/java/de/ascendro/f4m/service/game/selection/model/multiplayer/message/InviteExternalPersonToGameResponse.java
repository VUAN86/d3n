package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

/**
 * Content of inviteExternalPersonToGameResponse response
 * 
 */
public class InviteExternalPersonToGameResponse extends InviteResponse {

	private String personId;

	public InviteExternalPersonToGameResponse() {
		// initialize empty InviteExternalPersonToGameResponse
	}

	public String getPersonId() {
		return personId;
	}

	public void setPersonId(String personId) {
		this.personId = personId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InviteExternalPersonToGameResponse");
		builder.append(", personId=").append(personId);
		builder.append(", ").append(super.toString());
		builder.append("]");

		return builder.toString();
	}

}
