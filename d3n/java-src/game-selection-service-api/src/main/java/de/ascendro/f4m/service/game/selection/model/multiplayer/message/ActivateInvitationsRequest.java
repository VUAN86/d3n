package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Internal request to change state of invitations from PENDING to INVITED by
 * provided multiplayer game ID
 */
public class ActivateInvitationsRequest implements JsonMessageContent {

	private String mgiId;

	/**
	 * @param mgiId
	 *            multiplayer game ID
	 */
	public ActivateInvitationsRequest(String mgiId) {
		this.mgiId = mgiId;
	}

	public String getMgiId() {
		return mgiId;
	}

	public void setMgiId(String mgiId) {
		this.mgiId = mgiId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ActivateInvitationsRequest [mgiId=");
		builder.append(mgiId);
		builder.append("]");
		return builder.toString();
	}

}
