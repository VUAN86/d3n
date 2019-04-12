package de.ascendro.f4m.server.elastic;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringConnectionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticRestClientWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticRestClientWrapper.class);

	private final RestClient restClient;
	private MonitoringConnectionStatus lastStatus;
	
	public ElasticRestClientWrapper(Config config) {
		String strHosts = config.getProperty(ElasticConfigImpl.ELASTIC_SERVER_HOSTS);
		String[] hosts = strHosts == null ? new String[0] : strHosts.split(",");
		restClient = RestClient.builder(Arrays.stream(hosts).map(HttpHost::create).toArray(HttpHost[]::new)).build();
		lastStatus = MonitoringConnectionStatus.NC;
	}
	
	public Response performRequest(String method, String endpoint, Map<String, String> params,
            HttpEntity entity, Header... headers) throws IOException {
		return perform(() -> restClient.performRequest(method, endpoint, params, entity, headers), e -> true);
	}
	
	public Response performRequest(String method, String endpoint, Map<String, String> params,
			Function<IOException, Boolean> markConnectionNokOnError, Header... headers) throws IOException {
		return perform(() -> restClient.performRequest(method, endpoint, params, headers), markConnectionNokOnError);
	}
	
	public void close() throws IOException {
		lastStatus = MonitoringConnectionStatus.NC;
		restClient.close();
	}
	
	private <T> T perform(SupplierWithIOException<T> supplier, Function<IOException, Boolean> markConnectionNokOnError) throws IOException {
		try {
			T result = supplier.get();
			lastStatus = MonitoringConnectionStatus.OK;
			return result;
		} catch (IOException e) {
			if (markConnectionNokOnError.apply(e)) {
				lastStatus = MonitoringConnectionStatus.NOK;
			}
			throw e;
		}
	}
	
	public MonitoringConnectionStatus getLastStatus() {
		return lastStatus;
	}

	@FunctionalInterface
	private interface SupplierWithIOException<T> {
	    T get() throws IOException;
	}
}
