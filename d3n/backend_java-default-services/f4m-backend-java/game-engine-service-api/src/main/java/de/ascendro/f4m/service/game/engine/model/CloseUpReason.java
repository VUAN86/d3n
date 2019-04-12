package de.ascendro.f4m.service.game.engine.model;

public enum CloseUpReason {
	USER_CANCEL,
	NO_INVITEE, //No invited people showing up
	NO_PLAYER_COMPLETED, //No player completed the game
	BACKEND_FAILS, //Actual exception message expected
	EXPIRED; //Final close up of the game as its expired
}
