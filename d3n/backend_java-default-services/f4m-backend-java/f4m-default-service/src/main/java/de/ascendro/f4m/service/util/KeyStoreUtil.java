package de.ascendro.f4m.service.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.inject.Inject;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;

public class KeyStoreUtil {
	private final Config config;

	@Inject
	public KeyStoreUtil(Config config) {
		this.config = config;
	}

	/**
	 * Reads key store from key store provided path(@see Config.KEY_STORE) and initializes it via provided key(@see
	 * Config.KEY_STORE_PASSWORD)
	 * 
	 * @return Properly initialized KeyStore
	 * @throws F4MException
	 *             - failed to initialize KeyStore
	 */
	public KeyStore getKeyStore() throws F4MException {
		final KeyStore keyStore;
		final String keyStorePath = config.getProperty(Config.KEY_STORE);
		final String keyStorePassword = config.getProperty(Config.KEY_STORE_PASSWORD);

		try (final InputStream keyStoreInputStream = new FileInputStream(keyStorePath)) {
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(keyStoreInputStream, keyStorePassword.toCharArray());
		} catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
			throw new F4MFatalErrorException("Error while getting Key Store", e);
		}

		return keyStore;
	}

	/**
	 * Initializes Key Manager using Key Store and password provided
	 * 
	 * @return KeyManagers initialized
	 * @throws F4MException
	 *             - failed to initialize key store and key manager based on it.
	 */
	public KeyManager[] getKeyManagers() throws F4MException {
		KeyManagerFactory keyManagerFactory = null;
		final String keyStorePassword = config.getProperty(Config.KEY_STORE_PASSWORD);
		final KeyStore keyStore = getKeyStore();

		try {
			keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
		} catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
			throw new F4MFatalErrorException("Error while getting Key Managers", e);
		}

		return keyManagerFactory.getKeyManagers();
	}

	/**
	 * Initializes Trust Manager using Key Store and password provided
	 * 
	 * @return TrustManagers initialized
	 * @throws F4MException
	 *             - failed to initialize key store and trust manager based on it.
	 */
	public TrustManager[] getTrustManagers() throws F4MException {
		final TrustManagerFactory trustManagerFactory;
		final KeyStore keyStore = getKeyStore();

		try {
			trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(keyStore);
		} catch (NoSuchAlgorithmException | KeyStoreException e) {
			throw new F4MFatalErrorException("Error while getting Trust Managers", e);
		}

		return trustManagerFactory.getTrustManagers();
	}
}
