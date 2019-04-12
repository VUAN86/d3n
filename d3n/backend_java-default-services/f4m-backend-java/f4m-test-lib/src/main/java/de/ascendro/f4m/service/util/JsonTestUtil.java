package de.ascendro.f4m.service.util;

import static org.junit.Assert.assertEquals;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class JsonTestUtil {
	
	private static final String SEQ_BEGINNING = ",\"seq\":";
	private static final String TIMESTAMP_BEGINNING = ",\"timestamp\":";
	
	public interface JsonContentVerifier {
		void assertExpected(String expected, String actualAsString, JsonMessage<? extends JsonMessageContent> actualAsJsonMessage);
	}
	
	public static Gson getGson() {
		return new GsonBuilder().create();
	}

	public static String trimWhitespace(String json) {
		Pattern clean = Pattern.compile(" \\s+ | ( \" (?: [^\"\\\\] | \\\\ . ) * \" ) ",
				Pattern.COMMENTS | Pattern.DOTALL);
		StringBuffer sb = new StringBuffer();
		Matcher m = clean.matcher(json);
		while (m.find()) {
			m.appendReplacement(sb, "");
			// Don't put m.group(1) in the appendReplacement because if it
			// happens to contain $1 or $2 you'll get an error.
			if (m.group(1) != null) {
				sb.append(m.group(1));
			}
		}
		m.appendTail(sb);
		return sb.toString();
	}
	
	public static void assertJsonContentEqual(String expected, String actual) {
		assertJsonContentEqual(expected, actual, false);
	}

	public static void assertJsonContentEqualIgnoringSeq(String expected, String actual) {
		assertJsonContentEqual(expected, actual, true);
	}

	private static void assertJsonContentEqual(String expected, String actual, boolean ignoreSeq) {
		//XXX: With this approach, sequence becomes important. Consider parsing both and comparing data as objects
		//for example gson.toJson(gson.fromJson(registerRequest, Map.class))
		String expectedClean = trimWhitespace(expected);
		String actualClean = trimWhitespace(actual);
		if (ignoreSeq) {
			expectedClean = removeTimestampFromJson(removeSeqFromJson(expectedClean));
			actualClean = removeTimestampFromJson(removeSeqFromJson(actualClean));
		}
		assertEquals(expectedClean, actualClean);
	}
	
	private static String removeSeqFromJson(String cleanJson) {
		return removeFromJson(cleanJson, SEQ_BEGINNING);
	}
	
	private static String removeTimestampFromJson(String cleanJson) {
		return removeFromJson(cleanJson, TIMESTAMP_BEGINNING);
	}
	
	private static String removeFromJson(String cleanJson, String beginning) {
		String result;
		int seqStartIdx = cleanJson.indexOf(beginning);
		if (seqStartIdx >= 0) {
			int seqValueStartIdx = seqStartIdx + beginning.length();
			int seqEndIdx = cleanJson.length();
			seqEndIdx = getMinNonNegative(seqEndIdx, cleanJson.indexOf(",", seqValueStartIdx));
			seqEndIdx = getMinNonNegative(seqEndIdx, cleanJson.indexOf("}", seqValueStartIdx));
			result = cleanJson.substring(0, seqStartIdx) + cleanJson.substring(seqEndIdx);
		} else {
			result = cleanJson;
		}
		return result;
	}
	
	private static int getMinNonNegative(int x, int y) {
		int r;
		if (x >= 0) {
			if (y >= 0) {
				r = Math.min(x, y);
			} else {
				r = x;
			}
		} else {
			r = y;
		}
		return r;
	}
	
	public static String extractFieldValue(String json, String fieldName) {
		int start = json.indexOf(fieldName) + fieldName.length() + "\":\"".length();
		int end = json.indexOf('"', start);
		return json.substring(start, end);
	}
}
