package de.ascendro.f4m.service.payment.server;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.cache.AccountBalanceCache;
import de.ascendro.f4m.service.payment.model.PaymentClientInfo;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceRequest;

public class PaymentServiceServerMessageHandlerTest {
	private static final String TENANT_ID = "searchTenant";
	private static final String PROFILE_ID = "searchProfile";
	public static final String SEARCH_TENANT = "searchTenant";
	public static final String SEARCH_PROFILE = "searchProfile";
	@Mock
	private AccountBalanceCache accountBalanceCache;
	@InjectMocks
	private PaymentServiceServerMessageHandler handler;
	private ClientInfo clientInfo;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		clientInfo = new ClientInfo(TENANT_ID, "userHimself");
	}

//	@Test
//	public void onUserMessageFromService() {
//		ArgumentCaptor<PaymentClientInfo> clientArg = ArgumentCaptor.forClass(PaymentClientInfo.class);
//		GetAccountBalanceRequest balanceRequest = prepareAccountBalanceRequest(TENANT_ID);
//		JsonMessage<GetAccountBalanceRequest> message = new JsonMessage<>(PaymentMessageTypes.GET_ACCOUNT_BALANCE,
//				balanceRequest);
//		message.setClientInfo(new ClientInfo(SEARCH_TENANT, SEARCH_PROFILE));
//		executeOnMessageAndVerifyTenantAndProfile(clientArg, message);
//	}
//
//	@Test
//	public void onUserMessageFromConfigurator() {
//		ArgumentCaptor<PaymentClientInfo> clientArg = ArgumentCaptor.forClass(PaymentClientInfo.class);
//		GetAccountBalanceRequest balanceRequest = prepareAccountBalanceRequest("other");
//		JsonMessage<GetAccountBalanceRequest> message = new JsonMessage<>(PaymentMessageTypes.GET_ACCOUNT_BALANCE,
//				balanceRequest);
//		message.setClientInfo(clientInfo);
//		executeOnMessageAndVerifyTenantAndProfile(clientArg, message);
//	}
//
//	@Test
//	public void onGetAccountBalanceTenantThrowsException() {
//		ArgumentCaptor<PaymentClientInfo> clientArg = ArgumentCaptor.forClass(PaymentClientInfo.class);
//		GetAccountBalanceRequest balanceRequest = prepareAccountBalanceRequest(TENANT_ID);
//		balanceRequest.setProfileId(null);
//		JsonMessage<GetAccountBalanceRequest> message = new JsonMessage<>(PaymentMessageTypes.GET_ACCOUNT_BALANCE,
//				balanceRequest);
//		message.setClientInfo(clientInfo);
//		try {
//			executeOnMessageAndVerifyTenantAndProfile(clientArg, message);
//			fail();
//		} catch (F4MInsufficientRightsException e) {
//			assertEquals("ProfileId is mandatory", e.getMessage());
//		}
//	}

	private GetAccountBalanceRequest prepareAccountBalanceRequest(String tenantId) {
		GetAccountBalanceRequest balanceRequest = new GetAccountBalanceRequest();
		balanceRequest.setProfileId(PROFILE_ID);
		balanceRequest.setTenantId(tenantId);
		return balanceRequest;
	}

//	private void executeOnMessageAndVerifyTenantAndProfile(ArgumentCaptor<PaymentClientInfo> clientArg,
//			JsonMessage<GetAccountBalanceRequest> message) {
//		handler.onUserMessage(new RequestContext(message));
//		verify(accountBalanceCache).getAccountBalance(clientArg.capture(), any());
//		assertThat(clientArg.getValue().getTenantId(), equalTo(SEARCH_TENANT));
//		assertThat(clientArg.getValue().getProfileId(), equalTo(SEARCH_PROFILE));
//	}
}