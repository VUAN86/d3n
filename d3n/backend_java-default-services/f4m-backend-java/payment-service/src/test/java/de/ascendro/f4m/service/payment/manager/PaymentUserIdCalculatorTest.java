package de.ascendro.f4m.service.payment.manager;

import static de.ascendro.f4m.service.payment.manager.PaymentUserIdCalculator.isPaymentUserIdWithProfileId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PaymentUserIdCalculatorTest {

	@Test
	public void testPaymentUserIdCalculationForProfile() throws Exception {
		String tenantId = "tenantId";
		String profileId = "profileId";
		String paymentUserId = "tenantId_profileId";
		assertEquals(paymentUserId, PaymentUserIdCalculator.calcPaymentUserId(tenantId, profileId));
		assertEquals(tenantId, PaymentUserIdCalculator.calcTenantIdFromUserId(paymentUserId));
		assertEquals(profileId, PaymentUserIdCalculator.calcProfileIdFromUserId(paymentUserId));
	}

	@Test
	public void testPaymentUserIdCalculationForTenant() throws Exception {
		String tenantId = "tenantId";
		String profileId = null;
		String paymentUserId = "tenantId_";
		assertEquals(paymentUserId, PaymentUserIdCalculator.calcPaymentUserId(tenantId, profileId));
		assertEquals(tenantId, PaymentUserIdCalculator.calcTenantIdFromUserId(paymentUserId));
		assertNull(PaymentUserIdCalculator.calcProfileIdFromUserId(paymentUserId));
	}

	@Test
	public void testIsPaymentUserIdWithProfileId() throws Exception {
		assertTrue(isPaymentUserIdWithProfileId("tenantId_profileId"));
		assertFalse(isPaymentUserIdWithProfileId("gameId"));
		assertFalse(isPaymentUserIdWithProfileId("tenantId_"));
		assertFalse(isPaymentUserIdWithProfileId("_tenantId"));
		assertFalse(isPaymentUserIdWithProfileId("_"));
		assertFalse(isPaymentUserIdWithProfileId(null));
		assertFalse(isPaymentUserIdWithProfileId("tenantId_profileId_somethingStrange"));
	}
}
