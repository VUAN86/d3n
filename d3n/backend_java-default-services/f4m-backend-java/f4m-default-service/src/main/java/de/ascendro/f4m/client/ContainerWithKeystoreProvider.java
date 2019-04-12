package de.ascendro.f4m.client;

import javax.websocket.WebSocketContainer;

import de.ascendro.f4m.service.config.F4MConfig;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

/**
 * Code of class is based on jorg.eclipse.jetty.websocket.jsr356.JettyClientContainerProvider
 * although not implemented with service registration.
 */
public class ContainerWithKeystoreProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerWithKeystoreProvider.class);
	private Config config;

	public ContainerWithKeystoreProvider(Config config) {
		this.config = config;
	}

	public WebSocketContainer getWebSocketContainer() {
		ClientContainer container = new ClientContainer();
		SslContextFactory sslContextFactory = container.getClient().getSslContextFactory();
        String serviceName = config.getProperty(F4MConfig.SERVICE_NAME);
        if("someservice".equals(serviceName)) {
            try {
				KeyStore ks = createKeyStore(Paths.get(config.getProperty("system.ssl.reposotory")));
				container.getClient().getSslContextFactory().setKeyStore(ks);
            } catch (Exception e){
				LOGGER.error("Could not create keyStore for client container from \"system.ssl.reposotory\"");
            }
        } else {
			sslContextFactory.setKeyStorePath(config.getProperty(Config.KEY_STORE));
			sslContextFactory.setKeyStorePassword(config.getProperty(Config.KEY_STORE_PASSWORD));
		}
		try {
			container.start();
		} catch (Exception e) {
			throw new F4MFatalErrorException("Could not start client container", e);
		}
		return container;
	}

    private KeyStore createKeyStore(Path path)
            throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(path, "*.{crt,pem}")) {
            for (Path certPath : paths) {
                if (Files.isRegularFile(certPath)) {
                    try (InputStream inputStream = Files.newInputStream(certPath)) {
                        Certificate cert = certificateFactory.generateCertificate(inputStream);
                        String certName = certPath.getFileName().toString();
                        String alias = certName.substring(0, certName.lastIndexOf(".") - 1);
                        keyStore.setCertificateEntry(alias, cert);
                    } catch (Exception e) {
                        LOGGER.error("error, skipping cert, path keyStore.setCertificateEntry {}", e.getMessage());
                    }
                }
            }
        }
        return keyStore;
    }
}
