package de.ascendro.f4m.service.voucher.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.model.AppConfig;
import de.ascendro.f4m.server.profile.model.AppConfigApplication;
import de.ascendro.f4m.server.profile.model.AppConfigApplicationConfiguration;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;
import de.ascendro.f4m.service.voucher.model.UserVoucher;
import de.ascendro.f4m.service.voucher.model.Voucher;

@RunWith(MockitoJUnitRunner.class)
public class DependencyServicesCommunicatorTest {

	private static final String USER_ID = "user-id";
	private static final String CDN_BASE = "http://s3.com/";
	private static final String TENANT_ID = "tenant1";
	private static final String APP_ID = "appId1";
	private static final String RAW_IMAGE = "abc.jpeg";

	@Mock
	private JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;

	@Mock
	private ServiceRegistryClient serviceRegistryClient;

	@Mock
	private ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao;

	@Mock
	private JsonMessageUtil jsonUtil;

	private DependencyServicesCommunicator service;

	@Before
	public void setUp() throws Exception {
		this.service = new DependencyServicesCommunicator(serviceRegistryClient, jsonWebSocketClientSessionPool,
				jsonUtil, null, null, null, applicationConfigurationAerospikeDao);
	}

	@Test
	public void testSendBoughtVoucherAssignedEmailToUser() {
		ClientInfo clientInfo = Mockito.mock(ClientInfo.class);
		Mockito.when(clientInfo.getTenantId()).thenReturn(TENANT_ID);
		Mockito.when(clientInfo.getAppId()).thenReturn(APP_ID);
		UserVoucher userVoucher = Mockito.mock(UserVoucher.class);
		Voucher voucher = Mockito.mock(Voucher.class);
		Mockito.when(voucher.getBigImageId()).thenReturn(RAW_IMAGE);
		Mockito.when(voucher.getExpirationDate()).thenReturn("2018-01-01T10:00:00Z");

		AppConfig appConfig = Mockito.mock(AppConfig.class);
		Mockito.when(applicationConfigurationAerospikeDao.getAppConfiguration(Mockito.any(String.class),
				Mockito.any(String.class))).thenReturn(appConfig);

		AppConfigApplication appConfigApplication = Mockito.mock(AppConfigApplication.class);
		Mockito.when(appConfig.getApplication()).thenReturn(appConfigApplication);

		AppConfigApplicationConfiguration appConfigApplicationConfiguration = Mockito
				.mock(AppConfigApplicationConfiguration.class);
		Mockito.when(appConfigApplication.getConfiguration()).thenReturn(appConfigApplicationConfiguration);
		Mockito.when(appConfigApplicationConfiguration.getCdnMedia()).thenReturn(CDN_BASE);
		Answer<String> answer = new Answer<String>() {
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				SendEmailWrapperRequest emailRequest = invocation.getArgument(1);
				assertEquals(USER_ID, emailRequest.getUserId());
				String[] params = emailRequest.getParameters();
				assertThat(params[5], Matchers.containsString("/voucherBig/abc_medium.jpeg"));
				return null;
			}
		};
		Mockito.doAnswer(answer).when(jsonUtil).createNewMessage(Mockito.any(UserMessageMessageTypes.class),
				Mockito.any(SendEmailWrapperRequest.class));

		service.sendBoughtVoucherAssignedEmailToUser(USER_ID, userVoucher, voucher, clientInfo.getTenantId(),
				clientInfo.getAppId());

		Mockito.verify(serviceRegistryClient).getServiceConnectionInformation(UserMessageMessageTypes.SERVICE_NAME);
	}

}
