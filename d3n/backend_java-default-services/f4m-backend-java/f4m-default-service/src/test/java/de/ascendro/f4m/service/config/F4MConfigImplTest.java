package de.ascendro.f4m.service.config;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class F4MConfigImplTest {
	private F4MConfig f4mConfig;

	@Before
	public void setUp() throws Exception {
		f4mConfig = new F4MConfigImpl();
	}

	@Test
	public void testSetPropertyEmptyList() {
		final String property = UUID.randomUUID().toString();
		f4mConfig.setProperty(property, Collections.<String> emptyList());
		assertThat(f4mConfig.getPropertyAsListOfStrings(property), is(empty()));
	}

	@Test
	public void testSetPropertyList() {
		final String property = UUID.randomUUID().toString();
		final List<String> list = asList("B", "A", "D");

		f4mConfig.setProperty(property, list);
		assertThat(f4mConfig.getPropertyAsListOfStrings(property), equalTo(list));
	}

}
