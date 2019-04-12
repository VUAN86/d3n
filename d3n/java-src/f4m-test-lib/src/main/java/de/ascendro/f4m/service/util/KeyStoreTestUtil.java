package de.ascendro.f4m.service.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.integration.test.F4MIntegrationTestBase;
import de.ascendro.f4m.service.json.model.ISOCountry;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.json.model.user.UserRole;

public class KeyStoreTestUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreTestUtil.class);
	
	public static final String IP = "127.0.0.1";
	public static final String APP_ID = "60551c1e-a718-333-80f5-76304dec7eb7";
	public static final String TENANT_ID = "60551c1e-a718-444-80f5-76304dec7eb7";
	
	//Anonymous user
	public static final String ANONYMOUS_CLIENT_ID = "b0fd1c1e-a718-11e6-80f5-76304dec7eb7";
	public static final String ANONYMOUS_USER_ID = "a0fdfd8b-174c-4a80-9d2e-94cde52386d5";
	public static final String ANONYMOUS_USER_LANGUAGE = "en";
	public static final Double ANONYMOUS_USER_HANDICAP = 3.5d;
	public static final ClientInfo ANONYMOUS_CLIENT_INFO = new ClientInfo(ANONYMOUS_USER_ID);
	static{
		ANONYMOUS_CLIENT_INFO.setRoles(UserRole.ANONYMOUS.name());
		ANONYMOUS_CLIENT_INFO.setHandicap(ANONYMOUS_USER_HANDICAP);
		ANONYMOUS_CLIENT_INFO.setLanguage(ANONYMOUS_USER_LANGUAGE);
		ANONYMOUS_CLIENT_INFO.setAppId(APP_ID);
		ANONYMOUS_CLIENT_INFO.setTenantId(TENANT_ID);
		ANONYMOUS_CLIENT_INFO.setIp(IP);
		ANONYMOUS_CLIENT_INFO.setClientId(ANONYMOUS_CLIENT_ID);
		ANONYMOUS_CLIENT_INFO.setCountryCode(ISOCountry.DE);
		ANONYMOUS_CLIENT_INFO.setOriginCountry(ISOCountry.DE);
	}
		
	//Registered user
	public static final String REGISTERED_CLIENT_ID = "b1fd1c1e-a718-11e6-80f5-76304dec7eb7";
	public static final String REGISTERED_USER_ID = "a1fdfd8b-174c-4a80-9d2e-94cde52386d5";
	public static final String REGISTERED_USER_LANGUAGE = "de";
	public static final Double REGISTERED_USER_HANDICAP = 2.5d;
	public static final ClientInfo REGISTERED_CLIENT_INFO = new ClientInfo(REGISTERED_USER_ID);
	static{
		REGISTERED_CLIENT_INFO.setRoles(UserRole.REGISTERED.name());
		REGISTERED_CLIENT_INFO.setHandicap(REGISTERED_USER_HANDICAP);
		REGISTERED_CLIENT_INFO.setLanguage(REGISTERED_USER_LANGUAGE);
		REGISTERED_CLIENT_INFO.setAppId(APP_ID);
		REGISTERED_CLIENT_INFO.setTenantId(TENANT_ID);
		REGISTERED_CLIENT_INFO.setIp(IP);
		REGISTERED_CLIENT_INFO.setClientId(REGISTERED_CLIENT_ID);
		REGISTERED_CLIENT_INFO.setCountryCode(ISOCountry.DE);
	}

	//Admin
	public static final String ADMIN_CLIENT_ID = "b2fd1c1e-a718-11e6-80f5-76304dec7eb7";
	public static final String ADMIN_USER_ID = "a2fdfd8b-174c-4a80-9d2e-94cde52386d5";
	public static final String ADMIN_USER_LANGUAGE = "en";
	public static final Double ADMIN_USER_HANDICAP = 1.5d;
	public static final ClientInfo ADMIN_CLIENT_INFO = new ClientInfo(ADMIN_USER_ID);
	static{
		ADMIN_CLIENT_INFO.setRoles(UserRole.ADMIN.name());
		ADMIN_CLIENT_INFO.setHandicap(ADMIN_USER_HANDICAP);
		ADMIN_CLIENT_INFO.setLanguage(ADMIN_USER_LANGUAGE);
		ADMIN_CLIENT_INFO.setAppId(APP_ID);
		ADMIN_CLIENT_INFO.setTenantId(TENANT_ID);
		ADMIN_CLIENT_INFO.setIp(IP);
		ADMIN_CLIENT_INFO.setClientId(ADMIN_CLIENT_ID);
		ADMIN_CLIENT_INFO.setCountryCode(ISOCountry.DE);
	}
	
	//Fully registered user
	public static final String FULLY_REGISTERED_CLIENT_ID = "b3fd1c1e-a718-11e6-80f5-76304dec7eb7";
	public static final String FULLY_REGISTERED_USER_ID = "a3fdfd8b-174c-4a80-9d2e-94cde52386d5";
	public static final String FULLY_REGISTERED_USER_LANGUAGE = "de";
	public static final Double FULLY_REGISTERED_USER_HANDICAP = 5.5d;
	public static final ClientInfo FULLY_REGISTERED_CLIENT_INFO = new ClientInfo(FULLY_REGISTERED_USER_ID);
	static{
		FULLY_REGISTERED_CLIENT_INFO.setRoles(UserRole.FULLY_REGISTERED.name());
		FULLY_REGISTERED_CLIENT_INFO.setHandicap(FULLY_REGISTERED_USER_HANDICAP);
		FULLY_REGISTERED_CLIENT_INFO.setLanguage(FULLY_REGISTERED_USER_LANGUAGE);
		FULLY_REGISTERED_CLIENT_INFO.setAppId(APP_ID);
		FULLY_REGISTERED_CLIENT_INFO.setTenantId(TENANT_ID);
		FULLY_REGISTERED_CLIENT_INFO.setIp(IP);
		FULLY_REGISTERED_CLIENT_INFO.setClientId(FULLY_REGISTERED_CLIENT_ID);
		FULLY_REGISTERED_CLIENT_INFO.setCountryCode(ISOCountry.DE);
	}
	
	public static final String INVALID_ROLES_USER_ID = "00011100-111-0000-1111-000111000111";

	public static void initKeyStore(TemporaryFolder keystoreFolder) throws IOException {
		File keystoreFile = setupKeyStoreFile(keystoreFolder, "keystore.jks");
		LOGGER.debug("Writing keystore temporary copy to {}", keystoreFile.getAbsolutePath());
		System.setProperty(Config.TRUST_STORE, keystoreFile.getAbsolutePath());
		System.setProperty(Config.TRUST_STORE_PASSWORD, "f4mkey");

		System.setProperty(Config.KEY_STORE, keystoreFile.getAbsolutePath());
		System.setProperty(Config.KEY_STORE_PASSWORD, "f4mkey");
	}

	public static File setupKeyStoreFile(TemporaryFolder keystoreFolder, String keystoreFileName) throws IOException {
		File keystoreFile = new File(keystoreFolder.getRoot(), keystoreFileName);
		try (InputStream keystoreContent = F4MIntegrationTestBase.class
				.getResourceAsStream("/tls/" + keystoreFileName)) {
			Files.copy(keystoreContent, keystoreFile.toPath());
		}
		return keystoreFile;
	}
}
