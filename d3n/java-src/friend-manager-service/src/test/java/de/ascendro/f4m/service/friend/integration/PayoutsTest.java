package de.ascendro.f4m.service.friend.integration;

import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.APP_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.TENANT_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;

import com.google.inject.Injector;

import de.ascendro.f4m.server.analytics.model.RewardEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.model.AppConfigBuilder;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.UnexpectedTestException;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypes;
import de.ascendro.f4m.service.friend.model.api.AppRatedResponse;
import de.ascendro.f4m.service.friend.model.api.AppSharedResponse;
import de.ascendro.f4m.service.friend.model.api.DailyLoginResponse;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.internal.LoadOrWithdrawWithoutCoverageRequest;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

/**
 * Test class to test payout calls
 */
public class PayoutsTest extends FriendManagerTestBase {

	private static final BigDecimal BONUSPOINTS_FOR_DAILY_LOGIN = BigDecimal.valueOf(1);
	private static final BigDecimal BONUSPOINTS_FOR_RATING = BigDecimal.valueOf(2);
	private static final BigDecimal BONUSPOINTS_FOR_SHARING = BigDecimal.valueOf(3);
	private int payoutBonusCount;
	private List<UserActionPayout> payouts;
	private Tracker tracker;
	private ArgumentCaptor<ClientInfo> clientInfoArgumentCaptor;
	private ArgumentCaptor<RewardEvent> rewardEventArgumentCaptor;
	private ProfilePrimaryKeyUtil profilePrimaryKeyUtil;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		MockitoAnnotations.initMocks(this);
		clientInfoArgumentCaptor = ArgumentCaptor.forClass(ClientInfo.class);
		rewardEventArgumentCaptor = ArgumentCaptor.forClass(RewardEvent.class);

		payouts = new ArrayList<>();
        payoutBonusCount = 0;
		assertServiceStartup(PaymentMessageTypes.SERVICE_NAME);
		Injector injector = jettyServerRule.getServerStartup().getInjector();
		profilePrimaryKeyUtil = injector.getInstance(ProfilePrimaryKeyUtil.class);
		ApplicationConfigurationAerospikeDao appConfigDao = injector.getInstance(ApplicationConfigurationAerospikeDao.class);
		when(appConfigDao.getAppConfiguration(anyString(), anyString())).thenReturn(
				AppConfigBuilder.buildDefaultAppConfigWithBonusPayouts(BONUSPOINTS_FOR_DAILY_LOGIN, BONUSPOINTS_FOR_RATING,
						BONUSPOINTS_FOR_SHARING));
		tracker = injector.getInstance(Tracker.class);
	}

	private class UserActionPayout {
		private Currency currency;
		private BigDecimal amount;
		private String userId;

		private UserActionPayout(Currency currency, BigDecimal amount, String userId) {
			this.currency = currency;
			this.amount = amount;
			this.userId = userId;
		}

		public Currency getCurrency() {
			return currency;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public String getUserId() {
			return userId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			UserActionPayout that = (UserActionPayout) o;
			return currency == that.currency &&
					Objects.equals(amount, that.amount) &&
					Objects.equals(userId, that.userId);
		}
	}

	@Override
	protected JsonMessageContent onReceivedMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		PaymentMessageTypes paymentType = originalMessageDecoded.getType(PaymentMessageTypes.class);
		if (PaymentMessageTypes.TRANSFER_BETWEEN_ACCOUNTS == paymentType
				|| PaymentMessageTypes.LOAD_OR_WITHDRAW_WITHOUT_COVERAGE == paymentType) {
			return onTransferBetweenAccountsReceived(paymentType, originalMessageDecoded);
		} else {
			throw new UnexpectedTestException("Unexpected message " + originalMessageDecoded);
		}
	}

	private TransactionId onTransferBetweenAccountsReceived(PaymentMessageTypes paymentType,
															JsonMessage<? extends JsonMessageContent> originalMessageDecoded) throws F4MException {
		if (paymentType == PaymentMessageTypes.LOAD_OR_WITHDRAW_WITHOUT_COVERAGE) {
			LoadOrWithdrawWithoutCoverageRequest request = (LoadOrWithdrawWithoutCoverageRequest)
					originalMessageDecoded.getContent();
			if (StringUtils.isNotBlank(request.getToProfileId())) {
				addUserActionPayoutToExpected(request.getCurrency(), request.getAmount(), request.getToProfileId());
				payoutBonusCount++;
			}
		}
		return new TransactionId("123");
	}

	private void addUserActionPayoutToExpected(Currency currency, BigDecimal amount, String userId) {
		payouts.add(new UserActionPayout(currency, amount, userId));
	}

	private void addProfile(String userId) {
		Profile profile = new Profile();
		profile.setUserId(userId);
		String set = config.getProperty(AerospikeConfigImpl.AEROSPIKE_PROFILE_SET);
		String key = profilePrimaryKeyUtil.createPrimaryKey(userId);
		aerospikeDao.createJson(set, key, CommonProfileAerospikeDao.BLOB_BIN_NAME, profile.getAsString());
	}

	@Test
	public void testDailyLogin() throws IOException, URISyntaxException {
		addProfile(ANONYMOUS_USER_ID);

		String requestJson = getPlainTextJsonFromResources("emptyContentRequest.json",
				KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO).replaceFirst("<<messageName>>", "friend/dailyLogin");

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);

		assertReceivedMessagesWithWait(FriendManagerMessageTypes.DAILY_LOGIN_RESPONSE);

		JsonMessage<DailyLoginResponse> response = testClientReceivedMessageCollector.getMessageByType(
				FriendManagerMessageTypes.DAILY_LOGIN_RESPONSE);
		assertNull(response.getError());

		assertEquals(BONUSPOINTS_FOR_DAILY_LOGIN, response.getContent().getAmount());
		assertEquals(Currency.BONUS, response.getContent().getCurrency());
		assertFalse(response.getContent().isAlreadyLoggedInToday());

		// now send again to check that there isn't another payout in the same day. (might fail at midnight :) )
		testClientReceivedMessageCollector.clearReceivedMessageList();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);

		assertReceivedMessagesWithWait(FriendManagerMessageTypes.DAILY_LOGIN_RESPONSE);

		response = testClientReceivedMessageCollector.getMessageByType(
				FriendManagerMessageTypes.DAILY_LOGIN_RESPONSE);
		assertNull(response.getError());

		assertEquals(BigDecimal.ZERO, response.getContent().getAmount());
		assertEquals(Currency.BONUS, response.getContent().getCurrency());
		assertTrue(response.getContent().isAlreadyLoggedInToday());

		assertEquals(1, payoutBonusCount);
		assertEquals(1, payouts.size());
		assertUserActionPayout(payouts.get(0), BONUSPOINTS_FOR_DAILY_LOGIN);
		assertAnalyticsTrackerCalls(BONUSPOINTS_FOR_DAILY_LOGIN);
	}

	@Test
	public void testAppRated() throws IOException, URISyntaxException {
		String requestJson = getPlainTextJsonFromResources("emptyContentRequest.json",
				KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO).replaceFirst("<<messageName>>", "friend/appRated");

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);

		assertReceivedMessagesWithWait(FriendManagerMessageTypes.APP_RATED_RESPONSE);

		JsonMessage<AppRatedResponse> response = testClientReceivedMessageCollector.getMessageByType(
				FriendManagerMessageTypes.APP_RATED_RESPONSE);
		assertNull(response.getError());

		assertEquals(BONUSPOINTS_FOR_RATING, response.getContent().getAmount());
		assertEquals(Currency.BONUS, response.getContent().getCurrency());

		assertEquals(1, payoutBonusCount);
		assertEquals(1, payouts.size());
		assertUserActionPayout(payouts.get(0), BONUSPOINTS_FOR_RATING);
		assertAnalyticsTrackerCalls(BONUSPOINTS_FOR_RATING);

	}

	@Test
	public void testAppShared() throws IOException, URISyntaxException {
		String appSharedRequestJson = getPlainTextJsonFromResources("emptyContentRequest.json",
				KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO).replaceFirst("<<messageName>>", "friend/appShared");

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), appSharedRequestJson);

		assertReceivedMessagesWithWait(FriendManagerMessageTypes.APP_SHARED_RESPONSE);

		JsonMessage<AppSharedResponse> response = testClientReceivedMessageCollector.getMessageByType(
				FriendManagerMessageTypes.APP_SHARED_RESPONSE);
		assertNull(response.getError());

		assertEquals(BONUSPOINTS_FOR_SHARING, response.getContent().getAmount());
		assertEquals(Currency.BONUS, response.getContent().getCurrency());

		assertEquals(1, payoutBonusCount);
		assertEquals(1, payouts.size());
		assertUserActionPayout(payouts.get(0), BONUSPOINTS_FOR_SHARING);
		assertAnalyticsTrackerCalls(BONUSPOINTS_FOR_SHARING);
	}

	private void assertUserActionPayout(UserActionPayout userActionPayout, BigDecimal bonusAmount) {
		assertEquals(Currency.BONUS, userActionPayout.getCurrency());
		assertTrue(bonusAmount.compareTo(userActionPayout.getAmount()) == 0);
		assertEquals(KeyStoreTestUtil.ANONYMOUS_USER_ID, userActionPayout.getUserId());
	}

	private void assertAnalyticsTrackerCalls(BigDecimal amount) {
		verify(tracker, timeout(RetriedAssert.DEFAULT_TIMEOUT_MS).times(1))
				.addEvent(clientInfoArgumentCaptor.capture(), rewardEventArgumentCaptor.capture());
		assertEquals(1, clientInfoArgumentCaptor.getAllValues().size());

		assertEquals(0, amount.compareTo(BigDecimal.valueOf(rewardEventArgumentCaptor.getValue().getBonusPointsWon())));

		assertEquals(TENANT_ID, clientInfoArgumentCaptor.getValue().getTenantId());
		assertEquals(APP_ID, clientInfoArgumentCaptor.getValue().getAppId());
		assertEquals(ANONYMOUS_USER_ID, clientInfoArgumentCaptor.getValue().getUserId());
	}
}
