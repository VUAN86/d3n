package de.ascendro.f4m.service.registry;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;
import de.ascendro.f4m.service.registry.model.ServiceRegistryGetRequest;
import de.ascendro.f4m.service.registry.model.ServiceRegistryGetResponse;
import de.ascendro.f4m.service.registry.model.ServiceRegistryHeartbeatResponse;
import de.ascendro.f4m.service.registry.model.ServiceRegistryListRequest;
import de.ascendro.f4m.service.registry.model.ServiceRegistryListResponse;
import de.ascendro.f4m.service.registry.model.ServiceRegistryRegisterRequest;
import de.ascendro.f4m.service.registry.model.ServiceRegistryUnregisterEvent;
import de.ascendro.f4m.service.registry.model.monitor.InfrastructureStatisticsResponse;
import de.ascendro.f4m.service.registry.model.monitor.PushServiceStatistics;

public class ServiceRegistryMessageTypeMapper extends JsonMessageTypeMapImpl {
	private static final long serialVersionUID = 2536646576055488499L;
	
	public ServiceRegistryMessageTypeMapper() {
		init();
	}
	
	protected void init() {
		this.register(ServiceRegistryMessageTypes.REGISTER, new TypeToken<ServiceRegistryRegisterRequest>() { }.getType());
		this.register(ServiceRegistryMessageTypes.REGISTER_RESPONSE, new TypeToken<EmptyJsonMessageContent>() { }.getType());
		
		this.register(ServiceRegistryMessageTypes.GET, new TypeToken<ServiceRegistryGetRequest>() { }.getType());
		this.register(ServiceRegistryMessageTypes.GET_RESPONSE, new TypeToken<ServiceRegistryGetResponse>() { }.getType());
		
		this.register(ServiceRegistryMessageTypes.LIST, new TypeToken<ServiceRegistryListRequest>() { }.getType());
		this.register(ServiceRegistryMessageTypes.LIST_RESPONSE, new TypeToken<ServiceRegistryListResponse>() { }.getType());
		
		this.register(ServiceRegistryMessageTypes.UNREGISTER, new TypeToken<ServiceRegistryUnregisterEvent>() { }.getType());
		this.register(ServiceRegistryMessageTypes.UNREGISTER_RESPONSE, new TypeToken<EmptyJsonMessageContent>() { }.getType());
			
		this.register(ServiceRegistryMessageTypes.HEARTBEAT, new TypeToken<EmptyJsonMessageContent>() { }.getType());	
		this.register(ServiceRegistryMessageTypes.HEARTBEAT_RESPONSE, new TypeToken<ServiceRegistryHeartbeatResponse>() { }.getType());		

		this.register(ServiceRegistryMessageTypes.PUSH_SERVICE_STATISTICS, new TypeToken<PushServiceStatistics>() { });
		this.register(ServiceRegistryMessageTypes.GET_INFRASTRUCTURE_STATISTICS, new TypeToken<EmptyJsonMessageContent>() { }.getType());
		this.register(ServiceRegistryMessageTypes.GET_INFRASTRUCTURE_STATISTICS_RESPONSE, new TypeToken<InfrastructureStatisticsResponse>() { });		
}
}
