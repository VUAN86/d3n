package de.ascendro.f4m.service.json;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.json.validator.JsonMessageValidator;
import de.ascendro.f4m.service.util.ServiceUtil;

public class JsonMessageUtilTest {

	@Mock
	private GsonProvider gsonProvider;
	@Mock
	private ServiceUtil serviceUtil;
	@Mock
	private JsonMessageValidator jsonMessageValidator;
	
	private JsonMessageUtil jsonMessageUtil;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		jsonMessageUtil = new JsonMessageUtil(gsonProvider, serviceUtil, jsonMessageValidator);
	}

	@Test
	public void testValidatePermissions() {
		final JsonMessage<?> jsonMessage = new JsonMessage<>();
		jsonMessage.setClientId("anyClientId");
		jsonMessageUtil.validatePermissions(jsonMessage);
		verify(jsonMessageValidator, never()).validatePermissions(any(), any());
	}
	
	@Test
	public void testValidatePermissionsWithUserInfo() {
		final JsonMessage<?> jsonMessage = new JsonMessage<>();
		jsonMessage.setName("a");
		jsonMessage.setClientId("anyClientId");
		jsonMessage.getClientInfo().setUserId("anyUserId");
		jsonMessageUtil.validatePermissions(jsonMessage);
		verify(jsonMessageValidator, times(1)).validatePermissions("a", new String[0]);
	}
	
	@Test
	public void testValidatePermissionsWithUserInfoAndNoClientId() {
		final JsonMessage<?> jsonMessage = new JsonMessage<>();
		jsonMessage.setName("a");
		jsonMessage.setClientInfo(new ClientInfo("anyUserId"));
		jsonMessageUtil.validatePermissions(jsonMessage);
		verify(jsonMessageValidator, times(1)).validatePermissions("a", new String[0]);
	}
	
	@Test
	public void testValidatePermissionsWithUserInfoAndRoles() {
		final JsonMessage<?> jsonMessageWithClientId = new JsonMessage<>();
		jsonMessageWithClientId.setClientId("anyClientId");
		jsonMessageWithClientId.setClientInfo(new ClientInfo("anyUserId", new String[]{"anyRole"}));
		jsonMessageUtil.validatePermissions(jsonMessageWithClientId);
		verify(jsonMessageValidator, times(1)).validatePermissions(any(), any());
		
		reset(jsonMessageValidator);
		
		final JsonMessage<?> jsonMessageWithoutClientId = new JsonMessage<>();
		jsonMessageWithoutClientId.setClientInfo(new ClientInfo("anyUserId", new String[]{"anyRole"}));
		jsonMessageUtil.validatePermissions(jsonMessageWithoutClientId);
		verify(jsonMessageValidator, times(1)).validatePermissions(any(), any());
		
		reset(jsonMessageValidator);
		
		final JsonMessage<?> jsonMessageWithTenantId = new JsonMessage<>();
		final ClientInfo tenantClientInfo = new ClientInfo("anyUserId", new String[]{"TENANT_1_ADMIN", "TENANT_2_ADMIN", "TENANT_3_ADMIN"});
		tenantClientInfo.setTenantId("2");
		jsonMessageWithTenantId.setClientInfo(tenantClientInfo);
		jsonMessageUtil.validatePermissions(jsonMessageWithTenantId);
		
		ArgumentCaptor<String> rolesCaptor = ArgumentCaptor.forClass(String.class);
		verify(jsonMessageValidator, times(1)).validatePermissions(any(), rolesCaptor.capture());
		assertThat(rolesCaptor.getAllValues(), 
				containsInAnyOrder("TENANT_1_ADMIN", "TENANT_2_ADMIN", "TENANT_3_ADMIN", "ADMIN"));
		
		reset(jsonMessageValidator);
		rolesCaptor = ArgumentCaptor.forClass(String.class);
		jsonMessageWithTenantId.getClientInfo().setTenantId("4");
		jsonMessageUtil.validatePermissions(jsonMessageWithTenantId);
		verify(jsonMessageValidator, times(1)).validatePermissions(any(), rolesCaptor.capture());
		assertThat(rolesCaptor.getAllValues(), 
				containsInAnyOrder("TENANT_1_ADMIN", "TENANT_2_ADMIN", "TENANT_3_ADMIN"));
	}

}
