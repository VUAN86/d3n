package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

/**
 * Content of inviteExternalPersonToGameRequest response
 * 
 */
public class InviteExternalPersonToGameRequest extends InviteRequest {

	private String email;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InviteExternalPersonToGameRequest");
		builder.append(", email=").append(email);
		builder.append(", ").append(super.toString());
		builder.append("]");

		return builder.toString();
	}

}
