package de.ascendro.f4m.service.payment.di;

import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.payment.F4MPaymentException;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.openssl.PEMReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.inject.Inject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.Security;

public class RsaDecryptor {
	private static final Logger LOGGER = LoggerFactory.getLogger(RsaDecryptor.class);

	private final static String CIPHER_TYPE = "RSA";

	private PaymentConfig paymentConfig;

	@Inject
	public RsaDecryptor(PaymentConfig paymentConfig) {
		this.paymentConfig = paymentConfig;
	}

	public String encryptKey(String decrypted) throws F4MIOException {
		Key publicKey = readKey(paymentConfig.getProperty(PaymentConfig.PAYMENT_SYSTEM_PUBLIC_KEY_PATH),
				paymentConfig.getProperty(PaymentConfig.PAYMENT_SYSTEM_KEY_PASSWORD));
		return (new Base64()).encodeAsString(encrypt(publicKey, decrypted));
	}

	public String decryptKey(String encrypted) throws F4MIOException {
		Key privateKey = readKey(paymentConfig.getProperty(PaymentConfig.PAYMENT_SYSTEM_PRIVATE_KEY_PATH),
				paymentConfig.getProperty(PaymentConfig.PAYMENT_SYSTEM_KEY_PASSWORD));
		return decrypt(privateKey, (new Base64()).decode(encrypted));
	}

	private byte[] encrypt(Key publicKey, String text) {
		byte[] encrypted = null;
		try {
			Cipher rsa = Cipher.getInstance(CIPHER_TYPE);
			rsa.init(Cipher.ENCRYPT_MODE, publicKey);
			encrypted = rsa.doFinal(text.getBytes());
		} catch (GeneralSecurityException e) {
			throw new F4MPaymentException("Error encrypting message", e);
		}
		return encrypted;
	}

	private String decrypt(Key privateKey, byte[] buffer) {
		String decrypted = null;
		try {
			Cipher rsa = Cipher.getInstance(CIPHER_TYPE);
			rsa.init(Cipher.DECRYPT_MODE, privateKey);
			decrypted = new String(rsa.doFinal(buffer), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException e) {
			throw new F4MPaymentException("Error decrypting message", e);
		}
		return decrypted;
	}

	private Key readKey(String keyPath, String keyPassword) throws F4MIOException {
		if (StringUtils.isEmpty(keyPath)) {
			throw new F4MPaymentException("RSA key is not configured");
		}
		Security.addProvider(new BouncyCastleProvider());
		Key key = null;
		try (FileReader fileReader = new FileReader(new File(keyPath));
				PEMReader pemReader = new PEMReader(fileReader,
						() -> (keyPassword == null ? null : keyPassword.toCharArray()))) {
			Object keyObject = pemReader.readObject();
			if (keyObject instanceof X509CertificateObject) {
				// Public key
				key = ((X509CertificateObject) keyObject).getPublicKey();
			} else if (keyObject instanceof KeyPair) {
				// Private key
				key = ((KeyPair) keyObject).getPrivate();
			}
		} catch (IOException e) {
			throw new F4MIOException("Error reading certificate", e);
		}
		return key;
	}

}
