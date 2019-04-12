package de.ascendro.f4m.service.winning.client;

import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.MessageSource;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.winning.model.SuperPrize;
import de.ascendro.f4m.service.winning.model.UserWinningComponent;

public class UserWinningComponentRequestInfo extends PaymentRequestInfo {

	private final UserWinningComponent userWinningComponent;
	private final SuperPrize superPrize;
	
	public UserWinningComponentRequestInfo(JsonMessage<?> sourceMessage, MessageSource messageSource,
			UserWinningComponent userWinningComponent, SuperPrize superPrize) {
		super(sourceMessage, messageSource, userWinningComponent == null ? null : userWinningComponent.getGameId());
		this.userWinningComponent = userWinningComponent;
		this.superPrize = superPrize;
	}
	
	public UserWinningComponent getUserWinningComponent() {
		return userWinningComponent;
	}
	
	public SuperPrize getSuperPrize() {
		return superPrize;
	}
	
}
