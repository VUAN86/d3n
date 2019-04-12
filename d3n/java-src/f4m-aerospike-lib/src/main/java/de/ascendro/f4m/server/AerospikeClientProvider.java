package de.ascendro.f4m.server;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.policy.ClientPolicy;
import com.google.inject.Provider;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringConnectionStatus;
import de.ascendro.f4m.service.registry.model.monitor.ServiceStatistics;
import de.ascendro.f4m.service.util.register.ServiceMonitoringRegister;

public class AerospikeClientProvider implements Provider<IAerospikeClient> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AerospikeClientProvider.class);

	protected final Config config;
	protected IAerospikeClient aerospikeClient;

	@Inject
	public AerospikeClientProvider(Config config, ServiceMonitoringRegister serviceMonitoringRegister) {
		this.config = config;
		serviceMonitoringRegister.register(this::updateMonitoringStatistics);
	}

	@Override
	public IAerospikeClient get() {
		if (aerospikeClient == null) {
			initAerospikeClient();
		}
		return aerospikeClient;
	}

	protected void initAerospikeClient() {
		final String hostname = config.getProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_HOST);
		final Integer port = config.getPropertyAsInteger(AerospikeConfigImpl.AEROSPIKE_SERVER_PORT);
		final Integer clientPolicyTimeout = config
				.getPropertyAsInteger(AerospikeConfigImpl.AEROSPIKE_CLIENT_POLICY_TIMEOUT);
		
		LOGGER.debug("Initializing Aerospike client for host [{}], at port [{}]", hostname, port);

		final ClientPolicy policy = new ClientPolicy();
		policy.timeout = clientPolicyTimeout;
		policy.password = config.getProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_PASSWORD);
		policy.user = config.getProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_USER);
		aerospikeClient = new AerospikeClient(policy, hostname, port);
	}

	protected void updateMonitoringStatistics(ServiceStatistics statistics) {
		MonitoringConnectionStatus status;
		if (aerospikeClient != null) {
			status = aerospikeClient.isConnected() ? MonitoringConnectionStatus.OK : MonitoringConnectionStatus.NOK;
		} else {
			status = MonitoringConnectionStatus.NC;
		}
		statistics.getConnectionsToDb().setAerospike(status);
	}
}
