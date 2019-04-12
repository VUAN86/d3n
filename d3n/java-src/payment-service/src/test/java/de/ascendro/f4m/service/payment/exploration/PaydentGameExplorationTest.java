package de.ascendro.f4m.service.payment.exploration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.json.model.ISOCountry;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.callback.WaitingCallback;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentDetailsBuilder;
import de.ascendro.f4m.service.payment.model.TransactionFilterType;
import de.ascendro.f4m.service.payment.model.internal.CloseJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.CloseJackpotRequest.PayoutItem;
import de.ascendro.f4m.service.payment.model.internal.CloseJackpotResponse;
import de.ascendro.f4m.service.payment.model.internal.CreateJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.GameState;
import de.ascendro.f4m.service.payment.model.internal.GetJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.GetJackpotResponse;
import de.ascendro.f4m.service.payment.model.internal.TransferJackpotRequest;
import de.ascendro.f4m.service.payment.rest.model.AccountHistoryRest;
import de.ascendro.f4m.service.payment.rest.model.AccountHistoryRestSearchParams;
import de.ascendro.f4m.service.payment.rest.model.AccountRest;
import de.ascendro.f4m.service.payment.rest.model.TransactionRest;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class PaydentGameExplorationTest extends PaydentExplorationTestBase {
	private static final Logger LOGGER = LoggerFactory.getLogger(PaydentGameExplorationTest.class);

	@Test
	public void testCreateGetJackpotWithMoney() throws Exception {
		currencyManager.initSystemCurrencies(tenantId); //should not initialize in test, should work automatically on createJackpot!
		String gameId = "new_game_" + UUID.randomUUID();
		Currency currency = Currency.MONEY;
		createJackpot(gameId, currency);
		getJackpot(gameId, currency);
	}

	@Test
	public void testCreateGetBuyAndCloseJackpotWithBonus() throws Exception {
		currencyManager.initSystemCurrencies(tenantId); //should not initialize in test, should work automatically on createJackpot!
		//String gameId = "12345"; "12346"; "12347" - all are closed
		//String gameId = "12348"; //closed
		String multiplayerGameId = "bonus-game-" + UUID.randomUUID();
		Currency currency = Currency.BONUS;

		BigDecimal fee = new BigDecimal("3.25");
		BigDecimal payback = new BigDecimal("2.99");
		AccountRest account = userAccountManager.findActiveAccount(tenantId, profileId, currency);
		BigDecimal userBalance = account.getBalance();
		LOGGER.info("Account balance is {}", userBalance);
		if (userBalance.compareTo(fee) < 0) {
			LOGGER.info("Adding some money so that user can play");
			WaitingCallback<TransactionRest> callback = new WaitingCallback<>();
			transactionRestWrapper.loadOntoOrWithdrawFromAccount(account.getId(), BigDecimal.TEN, "Here is some money for gaming...", callback);
			callback.getResult();
			userBalance = userBalance.add(BigDecimal.TEN);
		}

		createJackpot(multiplayerGameId, currency);
		getJackpot(multiplayerGameId, currency);
		transferJackpot(multiplayerGameId, fee);
		closeJackpot(multiplayerGameId, payback);
		
		AccountRest userAccountAfterGame = userAccountManager.findActiveAccount(tenantId, profileId, currency);
		assertThat(userAccountAfterGame.getBalance(), equalTo(userBalance.subtract(fee).add(payback)));
		findGameTransactions(multiplayerGameId, currency);
	}

	private void createJackpot(String gameId, Currency money) {
		CreateJackpotRequest createRequest = new CreateJackpotRequest();
		createRequest.setCurrency(money);
		createRequest.setMultiplayerGameInstanceId(gameId);
		createRequest.setTenantId(tenantId);
		gameManager.createJackpot(createRequest);
	}

	private void getJackpot(String gameId, Currency currency) {
		GetJackpotRequest getRequest = new GetJackpotRequest();
		getRequest.setTenantId(tenantId);
		getRequest.setMultiplayerGameInstanceId(gameId);
		GetJackpotResponse getResponse = gameManager.getJackpot(getRequest);
		assertTrue(BigDecimal.ZERO.compareTo(getResponse.getBalance()) == 0);
		assertEquals(currency, getResponse.getCurrency());
		assertEquals(GameState.OPEN, getResponse.getState());
	}

	private void transferJackpot(String multiplayerGameId, BigDecimal fee) {
		TransferJackpotRequest transferJackpotRequest = new TransferJackpotRequest();
		transferJackpotRequest.setTenantId(tenantId);
		transferJackpotRequest.setMultiplayerGameInstanceId(multiplayerGameId);
		transferJackpotRequest.setFromProfileId(profileId);
		transferJackpotRequest.setAmount(fee);
		transferJackpotRequest.setPaymentDetails(
				new PaymentDetailsBuilder()
				.multiplayerGameInstanceId(multiplayerGameId)
				.appId("app")
				.additionalInfo("Some information to user about transfer").build());

		//Creating clientInfo with dummy data
		ClientInfo clientInfo=new ClientInfo();
		clientInfo.setAppId("1");
		clientInfo.setCountryCode(ISOCountry.DE);
		gameManager.transferJackpot(transferJackpotRequest,clientInfo);
	}

	private void closeJackpot(String gameId, BigDecimal payback) {
		CloseJackpotRequest closeJackpotRequest = new CloseJackpotRequest();
		closeJackpotRequest.setMultiplayerGameInstanceId(gameId);
		closeJackpotRequest.setTenantId(tenantId);
		closeJackpotRequest.setPaymentDetails(
				new PaymentDetailsBuilder().additionalInfo("Some information to user about payout").build());
		closeJackpotRequest.setPayouts(Arrays.asList(new PayoutItem(profileId, payback)));
		CloseJackpotResponse closeJackpotResponse = gameManager.closeJackpot(closeJackpotRequest);
		LOGGER.info("Jackpot {} closed with response {}", closeJackpotRequest, closeJackpotResponse);
	}

	private void findGameTransactions(String multiplayerGameId, Currency gameCurrencyEnum) throws Exception {
		AccountRest account = userAccountManager.findActiveAccount(tenantId, profileId, gameCurrencyEnum);

		AccountHistoryRestSearchParams search = new AccountHistoryRestSearchParams();
		search.setAccountId(account.getId());
		search.setStartDate(DateTimeUtil.getCurrentDateTime());
		search.setEndDate(DateTimeUtil.getCurrentDateTime().plusDays(1));
		search.setTypeFilter(TransactionFilterType.ALL);
		AccountHistoryRest history = accountRestWrapper.getAccountHistory(search);
		LOGGER.info("Here are transactions:");
		for (TransactionRest transaction : history.getData()) {
			LOGGER.info("{}", transaction);
		}
		assertEquals("Pay-in to jackpot transaction not found", 1, history.getData().stream()
				.filter(t -> multiplayerGameId.equals(t.getCreditorAccount().getUserId())).count());
		assertEquals("Pay-out to user transaction not found", 1, history.getData().stream().filter(
				t -> t.getDebitorAccount() != null && multiplayerGameId.equals(t.getDebitorAccount().getUserId()))
				.count());
	}
}
