package de.ascendro.f4m.service.payment.manager.impl;

import java.math.BigDecimal;

import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.manager.GameManager;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.internal.CloseJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.CloseJackpotResponse;
import de.ascendro.f4m.service.payment.model.internal.CreateJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.GameState;
import de.ascendro.f4m.service.payment.model.internal.GetJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.GetJackpotResponse;
import de.ascendro.f4m.service.payment.model.internal.TransferJackpotRequest;

public class GameManagerMockImpl implements GameManager {
	@Override
	public TransactionId transferJackpot(TransferJackpotRequest request, ClientInfo clientInfo) {
		return new TransactionId("transaction_id_value");
	}

	@Override
	public void createJackpot(CreateJackpotRequest request) {
	}

	@Override
	public GetJackpotResponse getJackpot(GetJackpotRequest request) {
		GetJackpotResponse response = new GetJackpotResponse();
		response.setBalance(BigDecimal.TEN.setScale(2));
		response.setCurrency(Currency.MONEY);
		response.setState(GameState.OPEN);
		return response;
	}

	@Override
	public CloseJackpotResponse closeJackpot(boolean isAdmin, CloseJackpotRequest request) {
		return new CloseJackpotResponse();
	}
}
