package de.ascendro.f4m.service.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class ServiceUtilTest {

	private ServiceUtil serviceUtil;

	@Before
	public void setUp() {
		serviceUtil = new ServiceUtil();
	}

	@Test
	public void testGetIntersectionOfSets() {
		Set<String> set1 = Stream.of("1", "2", "3").collect(Collectors.toCollection(HashSet::new));
		Set<String> set2 = Stream.of("2", "3", "4").collect(Collectors.toCollection(HashSet::new));
		Set<String> set3 = Stream.of("3", "4", "5").collect(Collectors.toCollection(HashSet::new));
		List<Set<String>> listOfSets = Stream.of(set1, set2, set3).collect(Collectors.toCollection(ArrayList::new));

		Set<String> intersectionSet = Stream.of("3").collect(Collectors.toCollection(HashSet::new));

		assertEquals(intersectionSet, serviceUtil.getIntersectionOfSets(listOfSets));
	}

}
