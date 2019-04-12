package de.ascendro.f4m.service.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.user.ClientInfo;

public class JsonLoader {	
	private static final String CLIENT_INFO_PATTERN = "\"" + JsonMessage.MESSAGE_CLIENT_INFO_PROPERTY
			+ "\":{\"profile\":{\"userId\":\"%s\",\"roles\":[%s],\"language\":\"%s\",\"handicap\":%s},"
			+ "\"appConfig\":{\"appId\":\"%s\",\"tenantId\":\"%s\"}, \"ip\":\"%s\",\"countryCode\":\"%s\", \"originCountry\":\"%s\"}";
	
	private static final String CLIENT_ID_KEY = "<<clientId>>";
	private static final String CLIENT_ID_PATTERN = "\"" + JsonMessage.MESSAGE_CLIENT_ID_PROPERTY + "\":\"" + CLIENT_ID_KEY + "\"";

	private Object testClass;

	public JsonLoader(Object testClass) {
		this.testClass = testClass;
	}
	
	public static String getTextFromResources(String path, Class<?> referenceClass) throws IOException {
		try (InputStream input = referenceClass.getResourceAsStream(path)) {
			return IOUtils.toString(input, "UTF-8");
		}
	}

	public String getPlainTextJsonFromResources(String path) throws IOException {
		return getPlainTextJsonFromResources(path, (ClientInfo)null);
	}

	public String getPlainTextJsonFromResources(String path, String userId) throws IOException {
		return getPlainTextJsonFromResources(path, new ClientInfo(userId));
	}
	
	public String getPlainTextJsonFromResources(String path, ClientInfo clientInfo) throws IOException {
		String plainTextJsonFromResources = getTextFromResources(path, testClass.getClass());
		if (plainTextJsonFromResources != null && clientInfo != null) {
			final int lastClosingBracketIndex = plainTextJsonFromResources.lastIndexOf('}');
			if(lastClosingBracketIndex > 0){
				final StringBuilder plainTextJsonBuilder = new StringBuilder(
						plainTextJsonFromResources.substring(0, lastClosingBracketIndex));
				if (StringUtils.isNotBlank(clientInfo.getClientId()) && !plainTextJsonFromResources.contains(JsonMessage.MESSAGE_CLIENT_ID_PROPERTY)) {
					plainTextJsonBuilder.append(',').append(CLIENT_ID_PATTERN.replace(CLIENT_ID_KEY, clientInfo.getClientId()));
				}
				if(!plainTextJsonFromResources.contains(JsonMessage.MESSAGE_CLIENT_INFO_PROPERTY)){
					final String clientInfoAsJsonString = String.format(CLIENT_INFO_PATTERN, 
							firstNonNull(clientInfo.getUserId(), "null"),
							Arrays.stream(firstNonNull(clientInfo.getRoles(), new String[0])).collect(Collectors.joining(", ", "\"", "\"")),
							firstNonNull(clientInfo.getLanguage(), "null"),
							clientInfo.getHandicap() != null ? String.format("%.2f", clientInfo.getHandicap()).replace(',', '.') : null,
							firstNonNull(clientInfo.getAppId(), "null"),
							firstNonNull(clientInfo.getTenantId(), "null"),
							firstNonNull(clientInfo.getIp(), "null"),
							firstNonNull(clientInfo.getCountryCodeAsString(), "null"),
							firstNonNull(clientInfo.getOriginCountryAsString(), "null")
						);
					plainTextJsonBuilder.append(',').append(clientInfoAsJsonString);
				}
	
				plainTextJsonFromResources = plainTextJsonBuilder.append('}').toString();
				plainTextJsonFromResources = plainTextJsonFromResources
						.replace("\"null\"", "null")
						.replace("[\"\"]", "null");
			}
		}
		return plainTextJsonFromResources;
	}

}
