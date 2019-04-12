package de.ascendro.f4m.server.elastic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.rules.ExternalResource;

/**
 * A JUnit rule that creates an elasticsearch client that writes to a temp dir. Inspired by
 * http://cupofjava.de/blog/2012/11/27/embedded-elasticsearch-server-for-tests/
 */
public class ElasticsearchTestNode extends ExternalResource {

	private Node node;
	private Path dataDirectory;
	private int port;
	
	public ElasticsearchTestNode(int port) {
		this.port = port;
	}
	
	@Override
	protected void before() {
		try {
			dataDirectory = Files.createTempDirectory("es-test", new FileAttribute<?>[] {});
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
		deleteDirectory();

		Settings settings = Settings.builder()
				.put("http.enabled", "true")
				.put("path.home", dataDirectory.toString())
				.put("http.port", port)
				.put("discovery.zen.ping.multicast.enabled", false)
				.put("index.number_of_shards", 1)
				.put("index.number_of_replicas", 0)
				.build();

		node = NodeBuilder.nodeBuilder().local(true).settings(settings).node();
	}

	@Override
	protected void after() {
		node.close();
		deleteDirectory();
	}

	private void deleteDirectory() {
		try {
			FileUtils.deleteDirectory(dataDirectory.toFile());
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	public Client getClient() {
		return node.client();
	}

}
