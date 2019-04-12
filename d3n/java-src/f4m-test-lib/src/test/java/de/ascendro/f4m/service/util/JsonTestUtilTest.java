package de.ascendro.f4m.service.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class JsonTestUtilTest {

	@Test
	public void testTrimWhiteSpace() throws Exception {
		String json = "{ m : \"random Text\"    }";
		assertEquals("{m:\"random Text\"}", JsonTestUtil.trimWhitespace(json));
	}
	
	@Test
	public void testJsonContentEqualIgnoringSeq() throws Exception {
		String expectedJson = "{m:\"random Text\",ack: 123}";
		String json = "{ m : \"random Text\", \n"
				+ " \"seq\":326728358206686,  "
				+ " ack: 123"
				+ "    }";
		JsonTestUtil.assertJsonContentEqualIgnoringSeq(expectedJson, json);
		json = "{ m : \"random Text\", \n"
				+ " ack: 123, "
				+ " \"seq\":326728358206686 "
				+ "    }";
		JsonTestUtil.assertJsonContentEqualIgnoringSeq(expectedJson, json);
	}
}
