package de.ascendro.f4m.service.registry.store;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import de.ascendro.f4m.service.registry.model.monitor.ServiceStatistics;

public class ServiceRegistryImplTest {
	
	private ServiceRegistryImpl serviceRegistry;

	@Before
	public void setUp() throws Exception {
		serviceRegistry = new ServiceRegistryImpl();
	}

	@Test
	public void getServiceStatisticsListWithServicesWithoutStatistics() {
		String serviceA = "serviceA";
		ServiceData serviceDataA1 = serviceRegistry.register(serviceA, "uri", Arrays.asList("A"), null);
		serviceDataA1.setLatestStatistics(prepareServiceStatistics(1));
		ServiceData serviceDataA2 = serviceRegistry.register(serviceA, "uri2", Arrays.asList("A"), null);
		serviceDataA2.setLatestStatistics(prepareServiceStatistics(2));
		serviceRegistry.register("serviceB", "uri3", Arrays.asList("B"), null);
		ServiceData serviceDataC2 = serviceRegistry.register("serviceC", "uri4", Arrays.asList("A"), null);
		serviceDataC2.setLatestStatistics(prepareServiceStatistics(3));
		
		assertThat(serviceRegistry.getServiceStatisticsList(), hasSize(3));
		assertThat(serviceRegistry.getServiceStatisticsList().stream().map(s -> s.getUptime())
				.collect(Collectors.toList()), containsInAnyOrder(1L, 2L, 3L));
	}

	private ServiceStatistics prepareServiceStatistics(int uptime) {
		ServiceStatistics serviceStatistics = new ServiceStatistics();
		serviceStatistics.setUptime(new Long(uptime));
		return serviceStatistics;
	}
}
