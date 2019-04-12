package de.ascendro.f4m.service.usermessage.translation;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.profile.model.Profile;

public class Translator {
    //Possible solution how to load external resource bundle files:
    // File file = new File("C:\\temp")
    // URL[] urls = {file.toURI().toURL()}
    // ClassLoader loader = new URLClassLoader(urls)
    // ResourceBundle rb = ResourceBundle.getBundle("myResource", Locale.getDefault(), loader)
	private static final Logger LOGGER = LoggerFactory.getLogger(Translator.class);
	protected static final List<ISOLanguage> SUPPORTED_LANGUAGES = Arrays.asList(ISOLanguage.EN, ISOLanguage.EL);
	public static final ISOLanguage DEFAULT_LANGUAGE = ISOLanguage.EN;
    public static final String TRANSLATION_BUNDLE = "Translations";
    
    public String translate(TranslatableMessage message) {
    	String[] params = message.getParameters() == null ? new String[0] : message.getParameters();
		ISOLanguage isoLanguage = message.getISOLanguage();
		if (isoLanguage == null) {
			if (ArrayUtils.isNotEmpty(params)) {
				MessageFormat formatter = new MessageFormat(message.getMessage(), null);
				LOGGER.debug("translate  formatter {} ", formatter);

				return formatter.format(params);
			} else {
				return message.getMessage();
			}
        } else {
            Locale locale = getLocale(isoLanguage);
            PropertyResourceBundle messages = getResourceBundle(locale);
			LOGGER.debug("translate  messages {} ", messages);

			String result = (String) messages.handleGetObject(message.getMessage());
			LOGGER.debug("translate  result {} ", result);
			MessageFormat formatter = new MessageFormat(result == null ? message.getMessage() : result, locale);
			LOGGER.debug("translate  formatter {} ", formatter);
			String[] translatedParams = Arrays.stream(params).map(f -> {
                String parameterResult = (String) messages.handleGetObject(f);
                return parameterResult == null ? f : parameterResult;
            }).toArray(String[]::new);
			LOGGER.debug("translate  translatedParams {} ", translatedParams);

			return formatter.format(translatedParams);
        }
    }

	protected PropertyResourceBundle getResourceBundle(Locale locale) {
		return (PropertyResourceBundle) PropertyResourceBundle.getBundle(TRANSLATION_BUNDLE, locale);
	}

	protected Locale getLocale(ISOLanguage isoLanguage) {
		return new Locale(isoLanguage.getValue());
	}

    public void updateLanguageFromProfile(Profile profile, TranslatableMessage message) {
		if (message.isLanguageAuto()) {
			ISOLanguage language = profile.getLanguage();
			if (language == null) {
				LOGGER.warn("Language not specified in profile {}, using default value {}", profile.getUserId(), DEFAULT_LANGUAGE);
				message.setISOLanguage(DEFAULT_LANGUAGE);
			} else {
				message.setISOLanguage(language); //not too clean to update input data, but effective
			}
		}
	}
    
    public Map<ISOLanguage, String> translateInAllLanguages(MessageWithParams message) {
    	TranslatableMessage temp = new TranslatableMessage();
    	temp.setMessage(message.getMessage());
    	temp.setParameters(message.getParameters());
    	Map<ISOLanguage, String> result = new HashMap<>();
    	for (ISOLanguage isoLanguage : SUPPORTED_LANGUAGES) {
    		temp.setISOLanguage(isoLanguage);
    		result.put(isoLanguage, translate(temp));
		}
    	return result;
    }
}
