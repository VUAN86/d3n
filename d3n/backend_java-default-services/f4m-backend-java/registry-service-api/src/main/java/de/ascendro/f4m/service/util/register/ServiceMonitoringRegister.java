package de.ascendro.f4m.service.util.register;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import de.ascendro.f4m.service.registry.model.monitor.ServiceStatistics;

public class ServiceMonitoringRegister {

	private List<ServiceMonitoringProvider> providers = new CopyOnWriteArrayList<>();
	
	public static interface ServiceMonitoringProvider extends Consumer<ServiceStatistics> {
	};
	
	public void register(ServiceMonitoringProvider provider) {
		providers.add(provider);
	}
	
	public void updateServiceStatisticsFromAllProviders(ServiceStatistics statistics) {
		providers.forEach(p -> p.accept(statistics));
	}
}
