package de.ascendro.f4m.service.payment.manager.impl;

import com.google.gson.Gson;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.game.engine.exception.F4MGameCancelledException;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.exception.F4MGameAccessException;
import de.ascendro.f4m.service.payment.manager.*;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.internal.*;
import de.ascendro.f4m.service.payment.payment.system.manager.GameBalanceManager;
import de.ascendro.f4m.service.payment.rest.wrapper.GameRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.RestWrapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class GameManagerImpl implements GameManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameManagerImpl.class);
	
	private CurrencyManager currencyManager;
    private AnalyticsEventManager analyticsEventManager;
	private RestWrapperFactory<GameRestWrapper> gameRestWrapperFactory;
	private Gson gson;

	@Inject
	private CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao;

	@Inject
	private GameBalanceManager gameBalanceManager;

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

//		GameRestBuyIn buyIn = new GameRestBuyIn();
//		buyIn.setGameId(loadOrWithdrawWithoutCoverage.getMultiplayerGameInstanceId());
//            buyIn.setUserId(PaymentUserIdCalculator.calcPaymentUserId(loadOrWithdrawWithoutCoverage.getTenantId(), loadOrWithdrawWithoutCoverage.getFromProfileId()));
//		buyIn.setAmount(loadOrWithdrawWithoutCoverage.getAmount());
//		buyIn.setReference(PaymentManagerImpl.prepareReferenceFromDetails(loadOrWithdrawWithoutCoverage.getPaymentDetails(), gson));
//		TransactionRest buyInGameTransaction = gameRestWrapperFactory.create(loadOrWithdrawWithoutCoverage.getTenantId()).buyInGame(buyIn);
//		LOGGER.debug("transferJackpot ");
//		return new TransactionId(buyInGameTransaction.getId());

		return gameBalanceManager.transferJackpot(request);
	}

	@Override
	public void createJackpot(CreateJackpotRequest request) {
//		CurrencyRest currencyRest = currencyManager.getCurrencyRestByCurrencyEnum(loadOrWithdrawWithoutCoverage.getTenantId(),
//				loadOrWithdrawWithoutCoverage.getCurrency());
//
//		GameRestInsert insert = new GameRestInsert();
//		insert.setCurrencyId(currencyRest.getId());
//		insert.setGameId(loadOrWithdrawWithoutCoverage.getMultiplayerGameInstanceId());
//
//		GameRestWrapper gameRestWrapper = gameRestWrapperFactory.create(loadOrWithdrawWithoutCoverage.getTenantId());
//		GameRest gameRest = gameRestWrapper.createGame(insert);
		if (request.getCurrency() != null && request.getMultiplayerGameInstanceId() != null && request.getTenantId() != null) {
			if (!gameBalanceManager.createJackpot(request, commonMultiplayerGameInstanceDao.getConfig(request.getMultiplayerGameInstanceId()))) {
				throw new F4MGameCancelledException("Game instance state is cancelled");
			}
		}

	}

	@Override
	public GetJackpotResponse getJackpot(GetJackpotRequest request) {
//		GameRestWrapper gameWrapper = gameRestWrapperFactory.create(loadOrWithdrawWithoutCoverage.getTenantId());
//		GameRest gameRest = gameWrapper.getGame(loadOrWithdrawWithoutCoverage.getMultiplayerGameInstanceId());
//		LOGGER.debug("getJackpot {}", gameRest);
		return gameBalanceManager.getJackpot(request);
	}

	@Override
	public CloseJackpotResponse closeJackpot(boolean isAdmin, CloseJackpotRequest request){
		LOGGER.debug("closeJackpot {} ", request);
		// Close jackpot tournament game available only for admins of this tenant
		if (isAdmin) {
			return gameBalanceManager.closeJackpot(request);
//		GameRestWrapper gameWrapper = gameRestWrapperFactory.create(loadOrWithdrawWithoutCoverage.getTenantId());
//		GameRestPayout gameRestPayout = new GameRestPayout();
//		gameRestPayout.setGameId(loadOrWithdrawWithoutCoverage.getMultiplayerGameInstanceId());
//		gameRestPayout.setReference(PaymentManagerImpl.prepareReferenceFromDetails(loadOrWithdrawWithoutCoverage.getPaymentDetails(), gson));
//		List<PayoutItem> payouts = loadOrWithdrawWithoutCoverage.getPayouts() != null ? loadOrWithdrawWithoutCoverage.getPayouts() : Collections.emptyList();
//		gameRestPayout.setPayouts(new ArrayList<>(payouts.size()));
//		for (PayoutItem payoutItem : payouts) {
//			GameRestPayoutItem item = new GameRestPayoutItem();
//			item.setAmount(payoutItem.getAmount());
//			item.setUserId(PaymentUserIdCalculator.calcPaymentUserId(loadOrWithdrawWithoutCoverage.getTenantId(), payoutItem.getProfileId()));
//			gameRestPayout.getPayouts().add(item);
//		}
//		GameRest gameRest = gameWrapper.endGame(gameRestPayout);
//
//		String  mgiId = loadOrWithdrawWithoutCoverage.getMultiplayerGameInstanceId();
//		commonMultiplayerGameInstanceDao.moveTournamentToFinished(commonMultiplayerGameInstanceDao.getConfigForFinish(mgiId).getGameId(), mgiId);
//
//
//		LOGGER.debug("closeJackpot gameRestPayout {} ", gameRestPayout);
//		LOGGER.info("Game {} successfully closed", gameRest);
////		} else {
////			throw new F4MInsufficientRightsException("ProfileId is mandatory");

		} else throw new F4MGameAccessException("Access error to the end of the game");
	}
}
