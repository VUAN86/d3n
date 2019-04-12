package de.ascendro.f4m.service.payment.manager;

import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.internal.*;

public interface BalanceManager {

    TransactionId transferJackpot(TransferJackpotRequest request, ClientInfo clientInfo);

    boolean createJackpot(CreateJackpotRequest request, CustomGameConfig customGameConfig);

    GetJackpotResponse getJackpot(GetJackpotRequest request);

    CloseJackpotResponse closeJackpot(boolean isAdmin, CloseJackpotRequest request);
}
