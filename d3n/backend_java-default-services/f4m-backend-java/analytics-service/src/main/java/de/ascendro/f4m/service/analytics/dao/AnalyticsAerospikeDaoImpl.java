package de.ascendro.f4m.service.analytics.dao;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.EventsDestinationMapper;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.analytics.ScanStatus;
import de.ascendro.f4m.service.analytics.activemq.IQueueBrokerManager;
import de.ascendro.f4m.service.analytics.activemq.impl.ActiveMqBrokerManager;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.config.Config;


public class AnalyticsAerospikeDaoImpl extends AerospikeDaoImpl<PrimaryKeyUtil<?>> implements AnalyticsAerospikeDao {
    @InjectLogger
    private static Logger LOGGER;
    private final IQueueBrokerManager activeMq;
    private final AtomicReference<ScanStatus> scanSatus = new AtomicReference<>(ScanStatus.STOPPED);

    @Inject
    public AnalyticsAerospikeDaoImpl(Config config, AerospikeClientProvider aerospikeClientProvider, JsonUtil jsonUtil,
                                     IQueueBrokerManager activeMq) {
        super(config, null, jsonUtil, aerospikeClientProvider);
        this.activeMq = activeMq;
    }

    @Override
    public void loadAnalyticsData() {
        if (scanSatus.compareAndSet(ScanStatus.SUSPENDED, ScanStatus.STOPPED)) {
            return;
        }

        Statement statement = new Statement();
        statement.setNamespace(getNamespace());
        statement.setSetName(AnalyticsDaoImpl.ANALYTICS_SET_NAME);

        try (RecordSet recordSet = getAerospikeClient().query(createQueryPolicy(), statement)) {
            //scan all the records in Aerospike for processing
            if(scanSatus.get() == ScanStatus.RUNNING) {
                scanRecords(recordSet);
            }
        }
    }

    @Override
    public void suspendScan() {
        scanSatus.compareAndSet(ScanStatus.RUNNING, ScanStatus.SUSPENDED);
    }

    @Override
    public void resumeScan() {
        scanSatus.compareAndSet(ScanStatus.STOPPED, ScanStatus.RUNNING);
    }

    @Override
    public boolean isRunning() {
        return scanSatus.get().equals(ScanStatus.RUNNING);
    }

    private void scanRecords(RecordSet recordSet) {
        try {
            while (recordSet.next()) {

                if (scanSatus.compareAndSet(ScanStatus.SUSPENDED, ScanStatus.STOPPED)) {
                    return;
                }

                Record record = recordSet.getRecord();
                Key key = recordSet.getKey();

                //send message to all listeners
                String value = new String((byte[]) record.bins.get(AnalyticsDaoImpl.CONTENT_BIN_NAME));
                EventContent content = jsonUtil.fromJson(value, EventContent.class);
                //send all messages for statistic processing
                if (EventsDestinationMapper.isStatisticEvent(content.getEventType())) {
                    activeMq.sendTopic(ActiveMqBrokerManager.STATISTIC_TOPIC, value);
                }
                //send all messages for notifications
                if (EventsDestinationMapper.isNotificationEvent(content.getEventType())) {
                    activeMq.sendTopic(ActiveMqBrokerManager.NOTIFICATION_TOPIC, value);
                }
                //send all messages for analytics SPARC processor
                if (EventsDestinationMapper.isSparkEvent(content.getEventType())) {
                    activeMq.sendTopic(ActiveMqBrokerManager.SPARK_TOPIC, value);
                }

                //send all messages for job processing
                if (EventsDestinationMapper.isJobEvent(content.getEventType())) {
                    activeMq.sendTopic(ActiveMqBrokerManager.JOB_TOPIC, value);
                }
                //delete record from Aerospike database
                deleteSilently(key);
            }
        } catch (Exception e) {
            LOGGER.error("Error scanning data", e);
        }
    }

    private QueryPolicy createQueryPolicy() {
        QueryPolicy policy = new QueryPolicy();
        policy.maxConcurrentNodes = 0;
        //default size is 5000
        policy.recordQueueSize = 10;

        return policy;
    }

}
