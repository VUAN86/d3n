package de.ascendro.f4m.server.elastic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;
import javax.ws.rs.HttpMethod;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;

import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.elastic.query.Query;
import de.ascendro.f4m.server.elastic.query.RootQuery;
import de.ascendro.f4m.server.elastic.query.Sort;
import de.ascendro.f4m.server.util.ElasticUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.registry.model.monitor.ServiceStatistics;
import de.ascendro.f4m.service.util.register.ServiceMonitoringRegister;

public class ElasticClient {

	private static final int DEFAULT_BATCH_SIZE = 300;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticClient.class);

	private static final String PATH_SEPARATOR = "/";
	private static final String ENDPOINT_SEARCH = "_search";
	private static final String ENDPOINT_MAPPING = "_mapping";
	private static final String ENDPOINT_HEALTH = "_cluster/health";

	private static final String HEADER_PARENT = "parent";
	private static final String HEADER_WAIT_FOR_STATUS = "wait_for_status";
	private static final String HEADER_REFRESH = "refresh";

	private static final Map<String, String> HEADERS_WAIT_FOR_YELLOW = Collections.singletonMap(HEADER_WAIT_FOR_STATUS, "yellow");
	
	private final ElasticRestClientWrapper restClient;
	private final ElasticUtil elasticUtil;
	public int batchSize;
	
	private Map<String, String> defaultHeaders;
	
	@Inject
	public ElasticClient(Config config, ElasticUtil elasticUtil, ServiceMonitoringRegister serviceMonitoringRegister) {
		restClient = new ElasticRestClientWrapper(config);
		serviceMonitoringRegister.register(this::updateMonitoringData);
		this.elasticUtil = elasticUtil;
		String refresh = config.getProperty(ElasticConfigImpl.ELASTIC_HEADER_REFRESH);
		defaultHeaders = StringUtils.isEmpty(refresh) ? Collections.emptyMap() : Collections.singletonMap(HEADER_REFRESH, refresh);
		this.batchSize = DEFAULT_BATCH_SIZE;
	}

	private void updateMonitoringData(ServiceStatistics statistics) {
		statistics.getConnectionsToDb().setElastic(restClient.getLastStatus());
	}

	public <T> List<T> queryAll(String index, String type, Query query, String orderField, Function<? super JsonObject, T> mapping) throws F4MFatalErrorException {
		List<T> result = new ArrayList<>();

		long offset = 0;
		ElasticResponse response;
		do {
			response = query(index, type, RootQuery.query(query).sort(Sort.newSort(orderField)).from(offset).size(batchSize));
			result.addAll(response.getResults().stream().map(mapping).collect(Collectors.toList()));
			offset += batchSize;
		} while (response.getResults().size() >= batchSize);
		
		return result;
	}
	
	public ElasticResponse query(String index, String type, RootQuery query) throws F4MFatalErrorException {
		String strQuery = query.getAsString();
		String strResponse;
		try (NStringEntity entity = new NStringEntity(strQuery, ContentType.APPLICATION_JSON)) {
			String endpoint = PATH_SEPARATOR + index + PATH_SEPARATOR + type +
					PATH_SEPARATOR + ENDPOINT_SEARCH;
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Performing elastic GET request to <{}>: {}", endpoint, strQuery);
			}
			try {
				Response response = restClient.performRequest(HttpMethod.GET, endpoint, defaultHeaders, entity);
				strResponse = EntityUtils.toString(response.getEntity());
			} catch (IOException e) {
				throw new F4MFatalErrorException("Error querying elastic", e);
			}
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Results: {}", strResponse);
		}
		return new ElasticResponse(elasticUtil.fromJson(strResponse, JsonObject.class));
	}

	public void delete(String index, String type, String id) throws F4MFatalErrorException {
		delete(index, type, id, null, false, false);
	}
	
	public void delete(String index, String type, String id, boolean wait) throws F4MFatalErrorException {
		delete(index, type, id, null, wait, false);
	}
	
	public void delete(String index, String type, String id, boolean wait, boolean silent) throws F4MFatalErrorException {
		delete(index, type, id, null, wait, silent);
	}
	
	public void delete(String index, String type, String id, String parentId, boolean wait, boolean silent) throws F4MFatalErrorException {
		String endpoint = PATH_SEPARATOR + index + PATH_SEPARATOR + type +
				PATH_SEPARATOR + id;
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Performing elastic DELETE request to <{}>", endpoint);
		}
		Map<String, String> headers = new HashMap<>(defaultHeaders);
		if (parentId != null) {
			headers.put(HEADER_PARENT, parentId);
		}
		if (wait) {
			headers.put(HEADER_REFRESH, ElasticConfigImpl.ELASTIC_HEADER_VALUE_WAIT_FOR);
		}
		try {
			restClient.performRequest(HttpMethod.DELETE, endpoint, headers, e -> shouldThrowFatalOnError(silent, e));
		} catch (IOException e) {
			if (shouldThrowFatalOnError(silent, e)) {
				throw new F4MFatalErrorException("Error deleting document with id=" + id + " from index " + index + " type " + type, e);
			} else {
				LOGGER.debug("Ignoring elastic exception while deleting document with id=" + id + " from index " + index
						+ " type " + type + " with silent {}", silent, e);
			}
		}
	}

	private boolean shouldThrowFatalOnError(boolean silent, IOException e) {
		return ! silent || ! (e instanceof ResponseException)
				|| ((ResponseException) e).getResponse().getStatusLine().getStatusCode() != HttpStatus.SC_NOT_FOUND;
	}
	
	public void createOrUpdate(String index, String type, String id, JsonElement json) throws F4MFatalErrorException {
		createOrUpdate(index, type, id, json, null);
	}

	public void createOrUpdate(String index, String type, String id, JsonElement json, String parentId) throws F4MFatalErrorException {
		String strJson = json.toString();
		try (NStringEntity entity = new NStringEntity(strJson, ContentType.APPLICATION_JSON)) {
			String endpoint = new StringBuilder(PATH_SEPARATOR).append(index).append(PATH_SEPARATOR)
					.append(type).append(PATH_SEPARATOR).append(id).toString();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Performing elastic PUT request to <{}>: {}", endpoint, strJson);
			}
			Map<String, String> headers = defaultHeaders;
			if (parentId != null) {
				headers = new HashMap<>(defaultHeaders);
				headers.put(HEADER_PARENT, parentId);
			}
			try {
				restClient.performRequest(HttpMethod.PUT, endpoint, headers, entity);
			} catch (IOException e) {
				throw new F4MFatalErrorException("Error adding document to index", e);
			}
		}
	}

	public boolean indexExists(String index) {
		String endpoint = new StringBuilder(PATH_SEPARATOR).append(index).toString();
		try {
			Response response = restClient.performRequest(HttpMethod.HEAD, endpoint, defaultHeaders, e -> false);
			return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
		} catch (IOException e) {
			LOGGER.warn("Error determining if index exists", e);
			return false;
		}
	}
	
	public void createIndex(String index, String indexCreateJson) throws F4MFatalErrorException {
		String endpoint = new StringBuilder(PATH_SEPARATOR).append(index).toString();
		try (NStringEntity entity = indexCreateJson == null ? null : new NStringEntity(indexCreateJson, ContentType.APPLICATION_JSON)) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Creating elastic index {}: {}", index, indexCreateJson);
			}
			restClient.performRequest(HttpMethod.PUT, endpoint, defaultHeaders, entity);
			waitForYellow(index);
		} catch(IOException e) {
			throw new F4MFatalErrorException("Error creating index", e);
		}
	}

	public void createMapping(String index, String type, String mappingCreateJson) throws F4MFatalErrorException {
		String endpoint = new StringBuilder(PATH_SEPARATOR).append(index).append(PATH_SEPARATOR).append(ENDPOINT_MAPPING)
				.append(PATH_SEPARATOR).append(type).toString();
		try (NStringEntity entity = mappingCreateJson == null ? null : new NStringEntity(mappingCreateJson, ContentType.APPLICATION_JSON)) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Creating elastic mapping for type {}, index {}: {}", type, index, mappingCreateJson);
			}
			restClient.performRequest(HttpMethod.PUT, endpoint, defaultHeaders, entity);
			waitForYellow(index);
		} catch(IOException e) {
			throw new F4MFatalErrorException("Error creating index", e);
		}
	}
	
	public void waitForYellow(String index) throws F4MFatalErrorException {
		StringBuilder healthEndpoint = new StringBuilder(PATH_SEPARATOR).append(ENDPOINT_HEALTH);
		if (StringUtils.isNotBlank(index)) {
			healthEndpoint.append(PATH_SEPARATOR).append(index);
		}
		try {
			restClient.performRequest(HttpMethod.GET, healthEndpoint.toString(), HEADERS_WAIT_FOR_YELLOW, e -> true);
		} catch (IOException e) {
			throw new F4MFatalErrorException("Error waiting for yellow cluster state", e);
		}
	}

	@PreDestroy
	public void close() {
		try {
			restClient.close();
		} catch (IOException e) {
			LOGGER.error("Error closing elasticsearch client", e);
		}
	}

	public void deleteIndex(String index) throws F4MFatalErrorException {
		String endpoint = new StringBuilder(PATH_SEPARATOR).append(index).toString();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Deleting index {}", index);
		}
		try {
			restClient.performRequest(HttpMethod.DELETE, endpoint, defaultHeaders, e -> true);
			waitForYellow(null);
		} catch (IOException e) {
			throw new F4MFatalErrorException("Error deleting index", e);
		}
	}

}
