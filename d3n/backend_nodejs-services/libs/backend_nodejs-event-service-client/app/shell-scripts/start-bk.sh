rm -rf tempdir_service_registry
mkdir tempdir_service_registry
cd tempdir_service_registry
wget http://ascendro.de/f4m/service-registry.zip
unzip service-registry.zip
java \
	-Djavax.net.ssl.trustStore=tls/keystore.jks \
	-Djavax.net.ssl.trustStorePassword=f4mkey \
	-Djavax.net.ssl.keyStore=tls/keystore.jks \
	-Djavax.net.ssl.keyStorePassword=f4mkey \
	-Df4m.conf=f4m.conf \
        -Djetty.httpConfig.securePort=$PORT_REGISTRY_SERVICE \
	-cp "service-registry-0.0.1-SNAPSHOT.jar:lib/*" de.ascendro.f4m.service.registry.ServiceRegistryStartup&
echo $! >>registry-service.pid
cd ..
rm -rf tempdir_event_service
mkdir tempdir_event_service
cd tempdir_event_service
wget http://ascendro.de/f4m/event-service.zip
unzip event-service.zip
java \
	-Djavax.net.ssl.trustStore=tls/keystore.jks \
	-Djavax.net.ssl.trustStorePassword=f4mkey \
	-Djavax.net.ssl.keyStore=tls/keystore.jks \
	-Djavax.net.ssl.keyStorePassword=f4mkey \
	-Df4m.conf=f4m.conf \
        -Djetty.httpConfig.securePort=$PORT_EVENT_SERVICE \
        -Dservice.registry.uris=wss://localhost:$PORT_REGISTRY_SERVICE \
	-cp "event-service-0.0.1-SNAPSHOT.jar:lib/*" de.ascendro.f4m.service.event.EventServiceStartup&
echo $! >>event-service.pid
echo $PORT_EVENT_SERVICE >>event-service.port
cd ..
rm -rf tempdir_event_service2
mkdir tempdir_event_service2
cd tempdir_event_service2
wget http://ascendro.de/f4m/event-service.zip
unzip event-service.zip
java \
	-Djavax.net.ssl.trustStore=tls/keystore.jks \
	-Djavax.net.ssl.trustStorePassword=f4mkey \
	-Djavax.net.ssl.keyStore=tls/keystore.jks \
	-Djavax.net.ssl.keyStorePassword=f4mkey \
	-Df4m.conf=f4m.conf \
        -Djetty.httpConfig.securePort=$PORT_EVENT_SERVICE2 \
        -Dservice.registry.uris=wss://localhost:$PORT_REGISTRY_SERVICE \
        -Devent.activemq.port=9445 \
	-cp "event-service-0.0.1-SNAPSHOT.jar:lib/*" de.ascendro.f4m.service.event.EventServiceStartup&
echo $! >>event-service2.pid
echo $PORT_EVENT_SERVICE2 >>event-service2.port
cd ..





