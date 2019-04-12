package de.ascendro.f4m.service.payment.exploration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.payment.F4MPaymentException;
import de.ascendro.f4m.service.payment.exception.F4MPaymentClientException;
import de.ascendro.f4m.service.payment.exception.PaymentServiceExceptionCodes;
import de.ascendro.f4m.service.payment.model.external.IdentificationMethod;
import de.ascendro.f4m.service.payment.model.external.PaymentTransactionType;
import de.ascendro.f4m.service.payment.rest.model.AccountRest;
import de.ascendro.f4m.service.payment.rest.model.IdentificationInitializationRest;
import de.ascendro.f4m.service.payment.rest.model.IdentificationResponseRest;
import de.ascendro.f4m.service.payment.rest.model.IdentityRest;
import de.ascendro.f4m.service.payment.rest.model.CashoutDataRequest;
import de.ascendro.f4m.service.payment.rest.model.PaymentTransactionInitializationRest;
import de.ascendro.f4m.service.payment.rest.model.PaymentTransactionRest;

/**
 * In this test class are located all tests, for whom an active response or interaction is necessary
 * either by setting breakpoints at specific places or by executing callbacks/forwards etc.
 */
public class PaydentExplorationTestForCallbacksOrDevInteraction extends PaydentExplorationTestBase {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(PaydentExplorationTestForCallbacksOrDevInteraction.class);
	
	@Test
	public void testManualUserVerification() throws Exception {
		Boolean property = Boolean.valueOf(System.getProperty("manuallyTestUserInitialization", Boolean.FALSE.toString()));
		Assume.assumeTrue("Person running this test is not ready to manually forward himself to URL", property);
		IdentificationInitializationRest request = new IdentificationInitializationRest();
		request.setUserId(userId);
		request.setMethod(IdentificationMethod.ID);
		//String url = "www.dummycallback.lv/";
		//request.setCallbackUrlSuccess(url + "success");
		//request.setCallbackUrlError(url + "error");
		String url = "https://services.proofit.lv/rais/"; //dummy callback
		request.setCallbackUrlSuccess(url);
		request.setCallbackUrlError(url);
        IdentificationResponseRest responseRest = identificationRestWrapper.startUserIdentificationToGetForwardURL(request);
        LOGGER.warn("To continue identification process, manually forward to %FrontendHost%/identification/index/{}", responseRest.getId());
        
        // Manually accept user after web form submitting (for testing only)
        identificationRestWrapper.approveUser(responseRest.getId());
	}
	
	@Test
	public void testPaymentTransactionInitialization() throws Exception {
		Boolean property = Boolean.valueOf(System.getProperty("manuallyTestPaymentTransactionInitialization", Boolean.FALSE.toString()));
		Assume.assumeTrue("Person running this test is not ready to manually forward himself to URL", property);
		PaymentTransactionInitializationRest request = new PaymentTransactionInitializationRest();
		String id = "1_148596029635363bf0a55-7b9a-457f-835f-901162b13dde";// userId;
		Optional<AccountRest> creditAccount = userRestWrapper.getUserActiveAccounts(id).stream()
				.filter(acc -> "EUR".equals(acc.getCurrency().getShortName())).findFirst();
		String accountId = creditAccount.get().getId();

		//id=9 => currencyId=1 EUR
		//id=10 => currencyId=2 EUR
		//String accountId = "10";
		//String accountId = "9";
		//FIXME: create / update / get a correct user!
		request.setAccountId(accountId);
		boolean payIn = false;
		if (payIn) {
			request.setType(PaymentTransactionType.CREDIT);
			request.setRate(new BigDecimal("1"));
		} else {
			request.setType(PaymentTransactionType.DEBIT);
			CashoutDataRequest cashoutData = new CashoutDataRequest();
			cashoutData.setBeneficiary(FIRST_NAME + " " + LAST_NAME);
			cashoutData.setIban("DE02120300000000202051");
			cashoutData.setBic("BYLADEM1001");
			request.setCashoutData(cashoutData);
		}
		request.setValue(new BigDecimal("1"));
		request.setReference("reference value");
		String url = "https://services.proofit.lv/rais/"; //dummy callback
		request.setCallbackUrlSuccess(url);// + "success");
		request.setCallbackUrlError(url);// + "error");
		request.setRedirectUrlSuccess("http://media1.s-nbcnews.com/i/MSNBC/Components/Video/201608/f_success_kid_160805.jpg");
		request.setRedirectUrlError("https://img.memesuper.com/94a320c2156c0a36d1ec34515b82b7b1_now-your-failure-is-complete-failure-memes_494-358.jpeg");
		PaymentTransactionRest paymentTransactionRest = paymentTransactionRestWrapper.startPaymentTransactionToGetForwardURL(request);
		LOGGER.info("Payment transaction initialization response {}", paymentTransactionRest);
	}
	
	@Ignore
	@Test
	public void testConnectionLost() {
		List<Integer> sizes = new ArrayList<Integer>();
		for (int i = 0; i < 3; i++) {
			int size = 0;
			try {
				if (1 == i) {
					LOGGER.debug("Manually terminate Internet connection (10 sec)");
					Thread.sleep(10 * 1000); //put a breakpoint here for testing and unplug the internet cable! 
				}
				if (2 == i) {
					LOGGER.debug("Manually enable Internet connection (10 sec)");
					Thread.sleep(10 * 1000); //put a breakpoint here for testing and plug the internet cable back in!
				}
				size = userRestWrapper.getUserActiveAccounts(userId).size();
			} catch (Exception e) {
				LOGGER.error("Exception while getting info from Payment System", e);
			}
			sizes.add(size);
		}
		LOGGER.debug("getUserActiveAccounts result count in each iteration: {}", sizes.toString());
		assertEquals(sizes.get(0), sizes.get(2));
	}
	
	@Test
	public void testGetIdentity() throws Exception {
		try {
			userRestWrapper.getUserIdentity("1_1483608213161624abad7-dbe8-4700-89d4-cab3bb0e1fdd");
			//should not fail with USER_NOT_FOUND - either success or USER_NOT_IDENTIFIED
		} catch (F4MPaymentClientException e) {
			assertEquals("USER_NOT_IDENTIFIED", e.getMessage());
		}
	}

	@Test
	public void verifyUserIdentityCall() {
		try {
			String identityId = userId;
			//String identityId = "00000000-e89b-12d3-a456-426655440000_123e4567-e89b-12d3-a456-426655440001";
			//"Identification":{"Id":
			//String identityId = "4e55f5f3-c12d-11e6-a280-d43d7ebed795";
			IdentityRest userIdentity = userRestWrapper.getUserIdentity(identityId);
			LOGGER.info("Identity read successfully {}", userIdentity);
		} catch (F4MPaymentClientException e) {
			assertEquals(PaymentServiceExceptionCodes.ERR_USER_NOT_IDENTIFIED, e.getCode());
		}
	}
	
	@Test
	public void testGetPaymentTransaction() throws Exception {
		String transactionId = "nonexistent";
		try {
			paymentTransactionRestWrapper.getPaymentTransaction(transactionId );
			fail("Exception is expected for non-existent transactionId");
		} catch (F4MPaymentException e) {
			assertEquals("Die Anforderung ist ung\u00FCltig. - null", e.getMessage()); //"Request is not valid"
		}
	}
}
