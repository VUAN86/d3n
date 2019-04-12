package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

/**
 * Content of createPublicGame response
 * 
 */
public class CreatePublicGameResponse extends InviteResponse {

	public CreatePublicGameResponse() {
		// Empty constructor of createPublicGame response
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CreatePublicGameResponse [");
		builder.append(super.toString());
		builder.append("]");

		return builder.toString();
	}

}
