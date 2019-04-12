package de.ascendro.f4m.service.dao.aerospike;

import javax.inject.Inject;

import com.aerospike.client.IAerospikeClient;
import com.google.inject.Provider;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.util.register.ServiceMonitoringRegister;

public class AerospikeClientMockProvider extends AerospikeClientProvider implements Provider<IAerospikeClient> {

	@Inject
	public AerospikeClientMockProvider(Config config, ServiceMonitoringRegister serviceMonitoringRegister) {
		super(config, serviceMonitoringRegister);
	}

	@Override
	public IAerospikeClient get() {
		if (aerospikeClient == null) {
			initAerospikeClient();
		}
		return aerospikeClient;
	}

	@Override
	protected void initAerospikeClient() {
		aerospikeClient = new AerospikeClientMock();
	}

}
