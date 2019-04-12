package de.ascendro.f4m.service.payment.manager;

import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.internal.CloseJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.CloseJackpotResponse;
import de.ascendro.f4m.service.payment.model.internal.CreateJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.GetJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.GetJackpotResponse;
import de.ascendro.f4m.service.payment.model.internal.TransferJackpotRequest;

public interface GameManager {

	TransactionId transferJackpot(TransferJackpotRequest request, ClientInfo clientInfo);

	void createJackpot(CreateJackpotRequest request);

	GetJackpotResponse getJackpot(GetJackpotRequest request);

	CloseJackpotResponse closeJackpot(boolean isAdmin, CloseJackpotRequest request);

}
