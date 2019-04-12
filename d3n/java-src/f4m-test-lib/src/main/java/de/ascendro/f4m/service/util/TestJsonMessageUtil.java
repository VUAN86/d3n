package de.ascendro.f4m.service.util;

import de.ascendro.f4m.service.json.JsonMessageUtil;

public class TestJsonMessageUtil extends JsonMessageUtil {

	public TestJsonMessageUtil() {
		super(new TestGsonProvider(), null, null);
	}

}
