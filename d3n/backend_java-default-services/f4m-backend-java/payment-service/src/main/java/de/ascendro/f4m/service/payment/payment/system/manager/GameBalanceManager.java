package de.ascendro.f4m.service.payment.payment.system.manager;


import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.internal.*;

public interface GameBalanceManager {

    TransactionId transferJackpot(TransferJackpotRequest request);

    TransactionId transferJackpot(LoadOrWithdrawWithoutCoverageRequest request, String mgiId);

    boolean createJackpot(CreateJackpotRequest request, CustomGameConfig customGameConfig);

    GetJackpotResponse getJackpot(GetJackpotRequest request);

    CloseJackpotResponse closeJackpot(CloseJackpotRequest request);

}
