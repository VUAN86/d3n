package de.ascendro.f4m.service.payment.manager.impl;

import com.google.gson.Gson;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.game.engine.exception.F4MGameCancelledException;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.manager.AnalyticsEventManager;
import de.ascendro.f4m.service.payment.manager.CurrencyManager;
import de.ascendro.f4m.service.payment.manager.GameManager;
import de.ascendro.f4m.service.payment.manager.PaymentUserIdCalculator;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.internal.*;
import de.ascendro.f4m.service.payment.model.internal.CloseJackpotRequest.PayoutItem;
import de.ascendro.f4m.service.payment.rest.model.*;
import de.ascendro.f4m.service.payment.rest.wrapper.GameRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.RestWrapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameManagerImpl implements GameManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameManagerImpl.class);
	
	private CurrencyManager currencyManager;
    private AnalyticsEventManager analyticsEventManager;
	private RestWrapperFactory<GameRestWrapper> gameRestWrapperFactory;
	private Gson gson;

	@Inject
	private CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao;

	@Inject
	public GameManagerImpl(CurrencyManager currencyManager, RestWrapperFactory<GameRestWrapper> gameRestWrapperFactory,
			GsonProvider gsonProvider, AnalyticsEventManager analyticsEventManager) {
		this.currencyManager = currencyManager;
		this.gameRestWrapperFactory = gameRestWrapperFactory;
		this.analyticsEventManager = analyticsEventManager;
		gson = gsonProvider.get();
	}


	@Override
	public TransactionId transferJackpot(TransferJackpotRequest request, ClientInfo clientInfo) {
        analyticsEventManager.savePaymentEvent(request, clientInfo);

		GameRestBuyIn buyIn = new GameRestBuyIn();
		buyIn.setGameId(request.getMultiplayerGameInstanceId());
            buyIn.setUserId(PaymentUserIdCalculator.calcPaymentUserId(request.getTenantId(), request.getFromProfileId()));
		buyIn.setAmount(request.getAmount());
		buyIn.setReference(PaymentManagerImpl.prepareReferenceFromDetails(request.getPaymentDetails(), gson));
		TransactionRest buyInGameTransaction = gameRestWrapperFactory.create(request.getTenantId()).buyInGame(buyIn);
		return new TransactionId(buyInGameTransaction.getId());
	}

	@Override
	public void createJackpot(CreateJackpotRequest request) {
		CurrencyRest currencyRest = currencyManager.getCurrencyRestByCurrencyEnum(request.getTenantId(),
				request.getCurrency());

		GameRestInsert insert = new GameRestInsert();
		insert.setCurrencyId(currencyRest.getId());
		insert.setGameId(request.getMultiplayerGameInstanceId());

		GameRestWrapper gameRestWrapper = gameRestWrapperFactory.create(request.getTenantId());
		GameRest gameRest = gameRestWrapper.createGame(insert);
		if (!gameRest.getState().equals(GameState.OPEN)) {
			throw new F4MGameCancelledException("Game instance state is cancelled");
		}
	}

	@Override
	public GetJackpotResponse getJackpot(GetJackpotRequest request) {
		GameRestWrapper gameWrapper = gameRestWrapperFactory.create(request.getTenantId());
		GameRest gameRest = gameWrapper.getGame(request.getMultiplayerGameInstanceId());
		GetJackpotResponse response = new GetJackpotResponse();
		response.setBalance(gameRest.getBalance());
		response.setCurrency(currencyManager.getCurrencyEnumByCurrencyRest(request.getTenantId(), gameRest.getCurrency()));
		response.setState(gameRest.getState());
		return response;
	}

	@Override
	public CloseJackpotResponse closeJackpot(boolean isAdmin, CloseJackpotRequest request){
		// Close jackpot tournament game available only for admins of this tenant
	//	if (isAdmin) {
		GameRestWrapper gameWrapper = gameRestWrapperFactory.create(request.getTenantId());
		GameRestPayout gameRestPayout = new GameRestPayout();
		gameRestPayout.setGameId(request.getMultiplayerGameInstanceId());
		gameRestPayout.setReference(PaymentManagerImpl.prepareReferenceFromDetails(request.getPaymentDetails(), gson));
		List<PayoutItem> payouts = request.getPayouts() != null ? request.getPayouts() : Collections.emptyList();
		gameRestPayout.setPayouts(new ArrayList<>(payouts.size()));
		for (PayoutItem payoutItem : payouts) {
			GameRestPayoutItem item = new GameRestPayoutItem();
			item.setAmount(payoutItem.getAmount());
			item.setUserId(PaymentUserIdCalculator.calcPaymentUserId(request.getTenantId(), payoutItem.getProfileId()));
			gameRestPayout.getPayouts().add(item);
		}
		GameRest gameRest = gameWrapper.endGame(gameRestPayout);

		String  mgiId = request.getMultiplayerGameInstanceId();
		commonMultiplayerGameInstanceDao.moveTournamentToFinished(commonMultiplayerGameInstanceDao.getConfigForFinish(mgiId).getGameId(), mgiId);


		LOGGER.debug("closeJackpot gameRestPayout {} ", gameRestPayout);
		LOGGER.info("Game {} successfully closed", gameRest);
//		} else {
//			throw new F4MInsufficientRightsException("ProfileId is mandatory");
//		}
		return new CloseJackpotResponse();
	}
}
