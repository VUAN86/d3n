package de.ascendro.f4m.service.json.validation;

import static de.ascendro.f4m.service.ping.model.JsonPingMessageContent.MESSAGE_WITH_PERMISSIONS_NAME;
import static de.ascendro.f4m.service.ping.model.JsonPingMessageContent.MESSAGE_NAME;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.validation.F4MValidationException;
import de.ascendro.f4m.service.json.model.user.UserPermission;
import de.ascendro.f4m.service.json.model.user.UserRole;
import de.ascendro.f4m.service.json.validator.JsonMessageValidator;
import de.ascendro.f4m.service.ping.PingPongMessageSchemaMapper;
import de.ascendro.f4m.service.util.JsonLoader;

public class JsonMessageValidatorTest {
	
	private JsonLoader jsonLoader;
	private Config config;
	private PingPongMessageSchemaMapper pingPongMessageSchemaMap;
	private JsonMessageValidator jsonMessageValidator;

	@Before
	public void setUp() throws Exception {
		jsonLoader = new JsonLoader(this);
		config = new F4MConfigImpl();
		
		pingPongMessageSchemaMap = new PingPongMessageSchemaMapper(){
			private static final long serialVersionUID = -5441698140664109386L;

			@Override
			protected Map<String, Set<String>> loadRolePermissions() {
				final Map<String, Set<String>> rolePermissions = new HashMap<>();
				rolePermissions.put(UserRole.ANONYMOUS.name(),
						new HashSet<>(Arrays.asList(UserPermission.TENANT_DATA_READ.name())));
				rolePermissions.put(UserRole.REGISTERED.name(),
						new HashSet<>(Arrays.asList(UserPermission.TENANT_DATA_WRITE.name())));
				rolePermissions.put(UserRole.NOT_VALIDATED.name(), new HashSet<>());
				return rolePermissions;
			}
		};
		jsonMessageValidator = new JsonMessageValidator(pingPongMessageSchemaMap, config);
	}

	@Test
	public void testValidAuthMessage() throws F4MValidationException, IOException {
		String message = jsonLoader.getPlainTextJsonFromResources("ValidAuthMessage.json");
		jsonMessageValidator.validate(message);
	}

	@Test(expected = F4MValidationException.class)
	public void testInvalidMessageMissingPart() throws F4MValidationException, IOException {
		String message = jsonLoader.getPlainTextJsonFromResources("InvalidMessageMissingPart.json");
		jsonMessageValidator.validate(message);
	}

	@Test(expected = F4MValidationException.class)
	public void testInvalidMessageMissingSubPart() throws F4MValidationException, IOException {
		String message = jsonLoader.getPlainTextJsonFromResources("InvalidMessageMissingSubPart.json");
		jsonMessageValidator.validate(message);
	}

	@Test(expected = F4MValidationException.class)
	public void testInvalidMessageWrongType() throws F4MValidationException, IOException {
		String message = jsonLoader.getPlainTextJsonFromResources("InvalidMessageWrongType.json");
		jsonMessageValidator.validate(message);
	}

	@Test
	public void testValidPermissions() {
		jsonMessageValidator.validatePermissions(MESSAGE_WITH_PERMISSIONS_NAME, UserRole.REGISTERED.name());
	}

	@Test
	public void testValidPermissionsForNoRoles() {
		jsonMessageValidator.validatePermissions(MESSAGE_NAME);
	}

	@Test
	public void testValidatePermissionsForNoRoles() {
		try {
			jsonMessageValidator.validatePermissions(MESSAGE_WITH_PERMISSIONS_NAME);
			fail("F4MInsufficientRightsException expected");
		} catch (F4MInsufficientRightsException rEx) {
		}
	}
	
	@Test
	public void testValidatePermissionsForInvalidUserRole() {
		try {
			jsonMessageValidator.validatePermissions(MESSAGE_WITH_PERMISSIONS_NAME, "anyRole");
			fail("F4MInsufficientRightsException expected");
		} catch (F4MInsufficientRightsException rEx) {
		}
	}
	
	@Test
	public void testValidatePermissionsForInsufficientRights() {
		try {
			jsonMessageValidator.validatePermissions(MESSAGE_WITH_PERMISSIONS_NAME, UserRole.ANONYMOUS.name());
			fail("F4MInsufficientRightsException expected");
		} catch (F4MInsufficientRightsException rEx) {
		}
	}
	
	@Test
	public void testValidatePermissionsForNoMappedRoles() {
		try {
			jsonMessageValidator.validatePermissions(MESSAGE_WITH_PERMISSIONS_NAME, UserRole.NOT_VALIDATED.name());
			fail("F4MInsufficientRightsException expected");
		} catch (F4MInsufficientRightsException rEx) {
		}
	}
}
