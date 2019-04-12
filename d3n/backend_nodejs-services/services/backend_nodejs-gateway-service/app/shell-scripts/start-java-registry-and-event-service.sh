rm -rf tempdir_service_registry
mkdir tempdir_service_registry
cd tempdir_service_registry
wget http://ascendro.de/f4m/service-registry.zip
#cp ../app/shell-scripts/service-registry.zip .
unzip service-registry.zip
java \
	-Djavax.net.ssl.trustStore=tls/keystore.jks \
	-Djavax.net.ssl.trustStorePassword=f4mkey \
	-Djavax.net.ssl.keyStore=tls/keystore.jks \
	-Djavax.net.ssl.keyStorePassword=f4mkey \
	-Df4m.conf=f4m.conf \
        -Df4m.logToFileEnabled=false \
        -Djetty.httpConfig.securePort=$PORT_REGISTRY_SERVICE \
	-cp "service-registry.jar:lib/*" de.ascendro.f4m.service.registry.ServiceRegistryStartup&
echo $! >>registry-service.pid
echo "------------------------STARTING REGISTRY SERVICE----------------"
sleep 10
cd ..
rm -rf tempdir_event_service
mkdir tempdir_event_service
cd tempdir_event_service
wget http://ascendro.de/f4m/event-service.zip
#cp ../app/shell-scripts/event-service.zip .
unzip event-service.zip
java \
	-Djavax.net.ssl.trustStore=tls/keystore.jks \
	-Djavax.net.ssl.trustStorePassword=f4mkey \
	-Djavax.net.ssl.keyStore=tls/keystore.jks \
	-Djavax.net.ssl.keyStorePassword=f4mkey \
	-Df4m.conf=f4m.conf \
        -Df4m.logToFileEnabled=false \
        -Djetty.httpConfig.securePort=$PORT_EVENT_SERVICE \
        -Dservice.registry.uris=wss://localhost:$PORT_REGISTRY_SERVICE \
	-cp "event-service.jar:lib/*" de.ascendro.f4m.service.event.EventServiceStartup&
echo $! >>event-service.pid
echo $PORT_EVENT_SERVICE >>event-service.port
echo "------------------------STARTING EVENT SERVICE1----------------"
sleep 10
cd ..



