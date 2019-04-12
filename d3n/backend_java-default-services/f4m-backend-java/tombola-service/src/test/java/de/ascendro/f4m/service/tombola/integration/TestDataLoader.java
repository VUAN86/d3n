package de.ascendro.f4m.service.tombola.integration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import de.ascendro.f4m.service.tombola.model.events.TombolaEvents;
import de.ascendro.f4m.service.util.JsonLoader;

public class TestDataLoader extends JsonLoader {

    private final static String SEPARATOR = "!";

    public TestDataLoader(TombolaServiceStartupTest testClass) {
        super(testClass);
    }

    public static Map<String, String> loadTestData(InputStream in) throws IOException {
        Map<String, String> testData = new HashMap<>();

        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        while ((line = br.readLine()) != null) {
            int separatorIndex = line.indexOf(SEPARATOR);
            if (separatorIndex > 0 && separatorIndex < line.length()) {
                String key = line.substring(0, separatorIndex);
                String value = line.substring(separatorIndex + 1, line.length());

                testData.put(key, value);
            }
        }

        return testData;
    }

    public String getTombolaDrawNotificationJson(String tombolaId, String tenantId) throws IOException {
        return getPlainTextJsonFromResources("tombolaDrawNotification.json")
                .replace("{topic}", TombolaEvents.getDrawEventTopic(tombolaId))
                .replace("{tombolaId}", tombolaId)
                .replace("{tenantId}", tenantId);
    }

    public String getTombolaOpenCheckoutNotificationJson(String tombolaId) throws IOException {
        return getPlainTextJsonFromResources("tombolaOpenOrCloseCheckoutNotification.json")
                .replace("{topic}", TombolaEvents.getOpenCheckoutTopic(tombolaId))
                .replace("{tombolaId}", tombolaId);
    }

    public String getTombolaCloseCheckoutNotificationJson(String tombolaId) throws IOException {
        return getPlainTextJsonFromResources("tombolaOpenOrCloseCheckoutNotification.json")
                .replace("{topic}", TombolaEvents.getCloseCheckoutTopic(tombolaId))
                .replace("{tombolaId}", tombolaId);
    }

}
