package de.ascendro.f4m.service.voucher.integration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class TestDataLoader {

    public final static String SEPARATOR = "!";

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

}