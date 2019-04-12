package de.ascendro.f4m.service.usermessage.translation;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.util.JsonTestUtil;

public class PaymentCallbackTranslationTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(PaymentCallbackTranslationTest.class);
	
	private Translator translator;
	private TranslatableMessage translatableMessage;
	private Gson gson;

	@Before
	public void setUp() {
		gson = JsonTestUtil.getGson();
		translator = new Translator();
		translatableMessage = new TranslatableMessage();
		translatableMessage.setISOLanguage(ISOLanguage.EN);
	}
	
	@Test
	public void testPaymentCallbackSuccessMessageJsonValidity() throws Exception {
		PaymentCallbackMessageContentLegacy callback = prepareMessageContentFromJson();
		callback.setType("PAYMENT_SUCCESS");
		callback.setText("We are happy to inform you that your payment transaction of 12.34 EUR has been successfully completed. "
				+ "Now you should see the changed values in your player account overview. Happy playing.");
		String messageFromJson = gson.toJson(callback);
		LOGGER.debug("Message for successful callback should be {}", messageFromJson);
		
		translatableMessage.setMessage(Messages.PAYMENT_SYSTEM_PAYMENT_SUCCESS_PUSH);
		setParams(callback);
		
		String translatedMessage = translator.translate(translatableMessage);
		assertEquals(messageFromJson, translatedMessage);
	}

	@Test
	public void testPaymentCallbackFailureMessageJsonValidity() throws Exception {
		PaymentCallbackMessageContentLegacy callback = prepareMessageContentFromJson();
		callback.setType("PAYMENT_FAILURE");
		callback.setText("During processing your payment or payment transfer of 12.34 EUR an error occurred. "
				+ "Please try again. If you still have issues, please connect to the support team via the support function in your app and report the issue. Thanks.");
		String messageFromJson = gson.toJson(callback);
		LOGGER.debug("Message for failed callback should be {}", messageFromJson);
		
		translatableMessage.setMessage(Messages.PAYMENT_SYSTEM_PAYMENT_ERROR_PUSH);
		setParams(callback);

		String translatedMessage = translator.translate(translatableMessage);
		assertEquals(messageFromJson, translatedMessage);
	}

	private PaymentCallbackMessageContentLegacy prepareMessageContentFromJson() {
		PaymentCallbackMessageContentLegacy callback = new PaymentCallbackMessageContentLegacy();
		callback.setTransactionId("transaction_id_value");
		callback.setAmount(new BigDecimal("12.34"));
		callback.setCurrency("EUR");
		return callback;
	}
	
	private void setParams(PaymentCallbackMessageContentLegacy callback) {
		translatableMessage.setParameters(new String[] {
			callback.getTransactionId(),
			callback.getAmount().toPlainString(),
			callback.getCurrency()
		});
	}

	/**
	 * Test data for old approach, where payment callback payload was embedded in the "message" as JSONified string.
	 * TODO: After front-end switches implementation to use "payload", 
	 * "message" should contain only human readable message (current attribute "text") and this test should be removed.
	 * see {@link PaymentCallbackMessagePayload} 
	 */
	public class PaymentCallbackMessageContentLegacy {
		private String type;
		private String transactionId;
		private BigDecimal amount;
		private String currency;
		private String text;

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getTransactionId() {
			return transactionId;
		}

		public void setTransactionId(String transactionId) {
			this.transactionId = transactionId;
		}
		
		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}

		public String getCurrency() {
			return currency;
		}

		public void setCurrency(String currency) {
			this.currency = currency;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}
}
