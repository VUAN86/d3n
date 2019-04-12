package de.ascendro.f4m.service.usermessage.translation;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.util.JsonTestUtil;

public class IdentificationCallbackTranslationTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(IdentificationCallbackTranslationTest.class);
	
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
	public void testIdentificationCallbackSuccessMessageJsonValidity() throws Exception {
		IdentificationCallbackMessageContent callback = new IdentificationCallbackMessageContent();
		callback.setType("IDENTIFICATION_SUCCESS");
		callback.setText("Your user identification request has been processed and we are happy to tell you that your"
				+ " identification request was successfully completed. Now you are able to get the funds you won paid out."
				+ " Thanks and good luck in you future games.");
		String messageFromJson = gson.toJson(callback);
		LOGGER.debug("Message for successful callback should be {}", messageFromJson);
		
		translatableMessage.setMessage(Messages.PAYMENT_SYSTEM_IDENTIFICATION_SUCCESS_PUSH);
		setParams(callback);
		
		String translatedMessage = translator.translate(translatableMessage);
		assertEquals(messageFromJson, translatedMessage);
	}

	@Test
	public void testIdentificationCallbackFailureMessageJsonValidity() throws Exception {
		IdentificationCallbackMessageContent callback = new IdentificationCallbackMessageContent();
		callback.setType("IDENTIFICATION_FAILURE");
		callback.setText("During the player identification of [profile.firstName] [profile.lastName]," //placeholder replacing is not tested here
				+ " there have been errors that made a player identification impossible. Please request a new player"
				+ " identification to ensure your funds being delivered to you successfully.");
		String messageFromJson = gson.toJson(callback);
		LOGGER.debug("Message for failed callback should be {}", messageFromJson);
		
		translatableMessage.setMessage(Messages.PAYMENT_SYSTEM_IDENTIFICATION_ERROR_PUSH);
		setParams(callback);

		String translatedMessage = translator.translate(translatableMessage);
		assertEquals(messageFromJson, translatedMessage);
	}

	private void setParams(IdentificationCallbackMessageContent callback) {
		//no params
	}

	/**
	 * Test data for old approach, where payment callback payload was embedded in the "message" as JSONified string.
	 * TODO: After front-end switches implementation to use "payload", 
	 * "message" should contain only human readable message (current attribute "text") and this test should be removed. 
	 */
	public class IdentificationCallbackMessageContent {
		private String type;
		private String text;

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}
}
