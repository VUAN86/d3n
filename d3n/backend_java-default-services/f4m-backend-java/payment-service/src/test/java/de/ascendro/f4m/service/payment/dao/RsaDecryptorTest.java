package de.ascendro.f4m.service.payment.dao;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import de.ascendro.f4m.service.payment.F4MPaymentException;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.di.RsaDecryptor;

public class RsaDecryptorTest {

	private PaymentConfig paymentConfig;
	private RsaDecryptor rsaDecryptor;

	@Before
	public void setUp() {
		this.paymentConfig = new PaymentConfig();
		rsaDecryptor = new RsaDecryptor(paymentConfig);

		String privateKeyPath = paymentConfig.getProperty(PaymentConfig.PAYMENT_SYSTEM_PRIVATE_KEY_PATH);
		String publicKeyPath = paymentConfig.getProperty(PaymentConfig.PAYMENT_SYSTEM_PUBLIC_KEY_PATH);
		Assume.assumeTrue("Authorization key path is not specified", StringUtils.isNotBlank(privateKeyPath));
		Assume.assumeTrue("Public key path is not specified", StringUtils.isNotBlank(publicKeyPath));

	}

	@Test
	public void encryptCircleTest() throws IOException {
		String str = "11111111-2222-3333-4444-555555555555";
		String encrypted = rsaDecryptor.encryptKey(str);
		String decrypted = rsaDecryptor.decryptKey(encrypted);
		assertEquals(str, decrypted);
	}

	@Test
	public void decryptTest() throws IOException {
		String encrypted = "bbojDuhZ+dPyTXTglEEFQVkLvI0LvmxZZQpcd1pdruyPxswvkXOq+cUUIuAXdYRh0ZPo2lo0tJ9KI4UnS"
				+ "y0tLgM3/gNnhP9rnZtxGH7vAgwYkd6QT0pDaHmtYKf72AizRfXyFuHqmfrIkBjmnVTcCD1/MgCtgS5Te+JWjzAr1t/"
				+ "ZZroyEGdrQKIFdXbCjrIoCIjd7cPzvlRe7bnmKz2mgsxvOcdauuitxOEDdymmA/DVbNHNTQQ/nDSc2tm6Epqc78unw"
				+ "dLiltRTX1DQsVU/pHLU6Nyz1TddDpgQqpOGuvJQCTmzRwJzxHdy3kCJ5e9ChtK/HeYb4qKBynpkpOgl0ozKpKVC6dV"
				+ "lpjtWUoGMyvlKdcuuhSHQoSPjGZ2lJ6bFuY5u6Nvo4D1KeOL/3fSCmdAd37Lq0Dg+Z1e3ZiArYVVpRV+rAysPI1bj9"
				+ "yodcsXyb8TLcwyAI/2GVUR94FQ/rOHLfbm4SHyB/TyzWAIh2ec9jxT0TB2lt+Cv8YqmNMd4CN7ZA7GBiVfGZU9x/r9"
				+ "/PaFK7S2UXnJ17aII92cN6JdLF7XI6DpOrvjrKa8dXxc7XRDyBhEDC27jdJlWPjKLz54g7qxFx9XO8ufBP5DtEbn9E"
				+ "dGciPGOEnz7xasDDH9xolA3XSs7CCclKDb27Fb9VwnXKJ/fngXl7IsQXYOk8ro=";
		String decrypted = rsaDecryptor.decryptKey(encrypted);
		assertEquals("55555555-4444-3333-2222-111111111111", decrypted);
	}

	@Test(expected=F4MPaymentException.class)
	public void decryptErrorTest() throws IOException {
		String encrypted = "bbojDuhZ+dPyTXTglEEFQVkb27Fb9VwnXKJ/fngXl7IsQXYOk8ro=";
		String decrypted = rsaDecryptor.decryptKey(encrypted);
		assertEquals(null, decrypted);
	}

}
