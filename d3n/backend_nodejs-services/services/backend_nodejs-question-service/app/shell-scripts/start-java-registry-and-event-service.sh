rm -rf tempdir_service_registry
mkdir tempdir_service_registry
cd tempdir_service_registry
#wget http://ascendro.de/f4m/service-registry.zip
cp ../app/shell-scripts/service-registry.zip .
unzip service-registry.zip
java \
	-Djavax.net.ssl.trustStore=tls/keystore.jks \
	-Djavax.net.ssl.trustStorePassword=f4mkey \
	-Djavax.net.ssl.keyStore=tls/keystore.jks \
	-Djavax.net.ssl.keyStorePassword=f4mkey \
	-Df4m.conf=f4m.conf \
        -Df4m.logToFileEnabled=false \
        -Djetty.httpConfig.securePort=$PORT_REGISTRY_SERVICE \
        -Dservice.host=localhost \
	-cp "service-registry.jar:lib/*" de.ascendro.f4m.service.registry.ServiceRegistryStartup&
echo $! >>registry-service.pid
echo "------------------------STARTING REGISTRY SERVICE----------------"
sleep 5
# EVENT SERVICE
cd ..
rm -rf tempdir_event_service
mkdir tempdir_event_service
cd tempdir_event_service
#wget http://ascendro.de/f4m/event-service.zip
cp ../app/shell-scripts/event-service.zip .
unzip event-service.zip
java \
	-Djavax.net.ssl.trustStore=tls/keystore.jks \
	-Djavax.net.ssl.trustStorePassword=f4mkey \
	-Djavax.net.ssl.keyStore=tls/keystore.jks \
	-Djavax.net.ssl.keyStorePassword=f4mkey \
	-Df4m.conf=f4m.conf \
        -Df4m.logToFileEnabled=false \
        -Djetty.httpConfig.securePort=$PORT_EVENT_SERVICE \
        -Dservice.host=localhost \
        -Dservice.registry.uris=wss://localhost:$PORT_REGISTRY_SERVICE \
	-cp "event-service.jar:lib/*" de.ascendro.f4m.service.event.EventServiceStartup&
echo $! >>event-service.pid
echo $PORT_EVENT_SERVICE >>event-service.port
echo "------------------------STARTING EVENT SERVICE1----------------"
sleep 5
# WORKFLOW SERVICE
cd ..
rm -rf tempdir_workflow_service
mkdir tempdir_workflow_service
cd tempdir_workflow_service
#wget http://ascendro.de/f4m/workflow-service.zip
cp ../app/shell-scripts/workflow-service.zip .
unzip workflow-service.zip
java \
	-Djavax.net.ssl.trustStore=tls/keystore.jks \
	-Djavax.net.ssl.trustStorePassword=f4mkey \
	-Djavax.net.ssl.keyStore=tls/keystore.jks \
	-Djavax.net.ssl.keyStorePassword=f4mkey \
	-Df4m.conf=f4m.conf \
        -Df4m.logToFileEnabled=false \
        -Djetty.httpConfig.securePort=$PORT_WORKFLOW_SERVICE \
        -Dservice.host=localhost \
        -Daerospike.server.host=localhost \
        -Daerospike.server.port=3000 \
        -Daerospike.namespace=test \
        -Dworkflow.dataSource.maxPoolSize=10 \
        -Dworkflow.dataSource.driverClassName=com.mysql.jdbc.Driver \
        -Dworkflow.dataSource.url=jdbc:mysql://localhost:336/workflow \
        -Dworkflow.hibernate.dialect=org.hibernate.dialect.MySQL5Dialect \
        -Dworkflow.dataSource.user=root \
        -Dworkflow.dataSource.password=masterkey \
        -Dworkflow.hibernate.hbm2ddl.auto=update \
        -Dservice.registry.uris=wss://localhost:$PORT_REGISTRY_SERVICE \
	-cp "workflow-service.jar:lib/*" de.ascendro.f4m.service.workflow.WorkflowServiceStartup&
echo $! >>workflow-service.pid
echo $PORT_EVENT_SERVICE >>workflow-service.port
echo "------------------------STARTING WORKFLOW SERVICE----------------"
sleep 5




