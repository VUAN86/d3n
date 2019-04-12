package de.ascendro.f4m.service.payment.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.config.PaymentConfig;

public class TransactionLogCacheManagerTest {
	private TransactionLogCacheManager cacheManager;

	@Before
	public void setUp() throws Exception {
		PaymentConfig config = new PaymentConfig();
		cacheManager = new TransactionLogCacheManager(config, new LoggingUtil(config));
	}

	@Test
	public void testRegisterAndPop() {
		String transactionId = "externalId";
		String logId = "logId";
		cacheManager.put(transactionId, logId);
		
		assertEquals(logId, cacheManager.popLogId(transactionId));
		
		assertNull(cacheManager.popLogId(transactionId));
	}

}
