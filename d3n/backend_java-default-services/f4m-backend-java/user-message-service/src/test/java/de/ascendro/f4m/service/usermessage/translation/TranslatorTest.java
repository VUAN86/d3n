package de.ascendro.f4m.service.usermessage.translation;

import static de.ascendro.f4m.service.usermessage.translation.Messages.EMAIL_TEMPLATE_HEADER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.json.model.ISOLanguage;

/**
 * Created by Alexandru on 10/20/2016.
 */
public class TranslatorTest {
	private static final String HELLO_ENGLISH = "Hello 17";
	private static final String HELLO_GREEK = "\u03A7\u03B1\u03AF\u03C1\u03B5\u03C4\u03B1\u03B9 17";

	private static final Logger LOGGER = LoggerFactory.getLogger(TranslatorTest.class);

    private Translator translator;
    private TranslatableMessage translatableMessage;

    @Before
    public void setUp() {
        translator = new Translator();
        translatableMessage = new TranslatableMessage();
        translatableMessage.setISOLanguage(ISOLanguage.EN);
        translatableMessage.setParameters(new String[] {"17"});
    }
    
    @Test
	public void verifyMissingTranslations() throws Exception {
    	PropertyResourceBundle bundleEN = translator.getResourceBundle(translator.getLocale(ISOLanguage.EN));
    	PropertyResourceBundle bundleEL = translator.getResourceBundle(translator.getLocale(ISOLanguage.EL));
    	List<String> keysEN = getKeys(bundleEN);
		assertThat(getKeys(bundleEL), Matchers.containsInAnyOrder(keysEN.toArray(new String[keysEN.size()])));
	}

	private List<String> getKeys(PropertyResourceBundle bundle) {
		List<String> list = new ArrayList<>();
    	Enumeration<String> keys = bundle.getKeys();
    	while (keys.hasMoreElements()) {
    		list.add(keys.nextElement());
    	}
    	Collections.sort(list); //not necessary, but with sorted keys it is easier to find discrepancies
    	return list;
	}
	
	@Test
	public void testTranslationsInAllLanguages() throws Exception {
		translatableMessage.setMessage(EMAIL_TEMPLATE_HEADER);
		Map<ISOLanguage, String> translation = translator.translateInAllLanguages(translatableMessage);
		assertEquals(HELLO_ENGLISH, translation.get(ISOLanguage.EN));
		assertEquals(HELLO_GREEK, translation.get(ISOLanguage.EL));
		assertEquals(2, translation.size());
	}

    @Test
    public void testTranslationsForNullLanguage() throws Exception {
        translatableMessage.setISOLanguage(null);
        translatableMessage.setParameters(new String[] {"17", "18"});
        this.checkTranslation("{0} is testing translations, new placeholder {1} is filled.", "17 is testing translations, new placeholder 18 is filled.");
    }

    @Test
    public void testTranslateEnglish() throws Exception {
        translatableMessage.setISOLanguage(ISOLanguage.EN);
        this.checkTranslation(EMAIL_TEMPLATE_HEADER, HELLO_ENGLISH);
    }

    @Test
    public void testTranslateGreek() throws Exception {
    	//command to convert utf-8 file to ISO-8859-1 supported by ResourceBundle
    	//native2ascii -encoding utf8 src/test/resources/Translations_el-utf8.txt src/main/resources/Translations_el.properties
        translatableMessage.setISOLanguage(ISOLanguage.EL);
        //bytes exactly as declared in Translations_el-utf8.txt
        String expectedFromUtf8 = new String(toByteArray(0xce, 0xa7 , 0xce , 0xb1 , 0xce , 0xaf , 0xcf , 0x81 , 0xce , 0xb5 , 0xcf , 0x84 , 0xce , 0xb1 , 0xce , 0xb9 ), Charset.forName("UTF-8"));
        this.checkTranslation(EMAIL_TEMPLATE_HEADER, expectedFromUtf8 + " 17");
        //in unicode according to tables
        this.checkTranslation(EMAIL_TEMPLATE_HEADER, HELLO_GREEK);
    }

    @Test
    public void testTranslateUnsupportedLanguage() throws Exception {
        translatableMessage.setISOLanguage(ISOLanguage.LV);
        this.checkTranslation(EMAIL_TEMPLATE_HEADER, HELLO_ENGLISH); //fallback to english
    }

    private void checkTranslation(String translationKey, String translationText) {
        translatableMessage.setMessage(translationKey);
        String translatedMessage = translator.translate(translatableMessage);
        LOGGER.debug("{} {} <- translation", stringToBytesHex(translatedMessage), translatedMessage);
        LOGGER.debug("{} {} <- expected", stringToBytesHex(translationText), translationText);
        assertEquals(translationText, translatedMessage);
    }
    
    private String stringToBytesHex(String translatedMessage) {
    	StringBuilder sb = new StringBuilder();
        for (int i = 0; i < translatedMessage.length(); i++) {
        	sb.append(Integer.toHexString((int)translatedMessage.charAt(i)));
        	sb.append(' ');
		}
        return sb.toString();
    }
    
	private byte[] toByteArray(int... bytes) {
		byte[] result = new byte[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			result[i] = (byte) bytes[i];
		}
		return result;
	}
}
