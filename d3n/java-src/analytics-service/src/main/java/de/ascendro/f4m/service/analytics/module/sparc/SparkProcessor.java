package de.ascendro.f4m.service.analytics.module.sparc;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.service.analytics.activemq.IQueueBrokerManager;
import de.ascendro.f4m.service.analytics.activemq.impl.ActiveMqBrokerManager;
import de.ascendro.f4m.service.analytics.activemq.router.AnalyticMessageListener;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.module.BaseModule;
import de.ascendro.f4m.service.analytics.module.sparc.advisory.ConsumerAdvisoryListener;
import de.ascendro.f4m.service.analytics.module.sparc.advisory.NoConsumerAdvisoryListener;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;

public class SparkProcessor extends BaseModule implements AnalyticMessageListener {
    @InjectLogger
    private static Logger LOGGER;
    private static final Map<String, List<Field>> fieldsCache = new HashMap<>();
    private final IQueueBrokerManager activeMq;
    private Gson gson;
    private final ConsumerAdvisoryListener consumerAdvisoryListener;
    private final NoConsumerAdvisoryListener noConsumerAdvisoryListener;

    @Inject
    public SparkProcessor(IQueueBrokerManager activeMq, ConsumerAdvisoryListener consumerAdvisoryListener,
                          NoConsumerAdvisoryListener noConsumerAdvisoryListener, NotificationCommon notificationUtil) {
        super(notificationUtil);
        this.activeMq = activeMq;
        this.consumerAdvisoryListener = consumerAdvisoryListener;
        this.noConsumerAdvisoryListener = noConsumerAdvisoryListener;
        gson = new GsonBuilder().serializeNulls().create();
    }

    @Override
    public void initAdvisorySupport() throws JMSException {
        activeMq.startAdvisory(ActiveMqBrokerManager.TOPIC_ROOT + ".>", consumerAdvisoryListener);
        activeMq.startAdvisory(ActiveMqBrokerManager.SPARK_EXTERNAL_TOPIC, noConsumerAdvisoryListener);
    }

    @Override
    public void onModuleMessage(Message message) {
        final TextMessage textMessage = (TextMessage) message;
        try {
            //send all messages for Apache Spark analytics
            EventContent content = gson.fromJson(textMessage.getText(), EventContent.class);
            activeMq.sendTopic(ActiveMqBrokerManager.SPARK_EXTERNAL_TOPIC, prepareSparkStreamJson(content));
            LOGGER.debug("Message received: {} ", textMessage.getText());
        } catch (Exception e) {
            LOGGER.error("Error on message type", e);
        }
    }

    protected String prepareSparkStreamJson(EventContent eventContent) {
        try {
            if (fieldsCache.get(eventContent.getEventType())==null){
                fieldsCache.put(eventContent.getEventType(),
                        getAllFields(new LinkedList<>(), Class.forName(eventContent.getEventType())));
            }

            for (Field field : fieldsCache.get(eventContent.getEventType())) {
                JsonObject jsonObject = eventContent.getEventData().getJsonObject();
                prepareJsonField(jsonObject, field);
            }
        } catch (ClassNotFoundException ex) {
            LOGGER.error("Error preparing spark stream", ex);
        }
        return gson.toJson(eventContent);
    }

    private List<Field> getAllFields(List<Field> fields, Class<?> type) {
        for(Field field:type.getDeclaredFields()){
            field.setAccessible(true);
            if (Modifier.isStatic(field.getModifiers())
                    && String.class.isAssignableFrom(field.getType())) {
                fields.add(field);
            }
        }

        if (type.getSuperclass() != null && Object.class != type.getSuperclass()) {
            getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }

    private void prepareJsonField(JsonObject jsonObject, Field field) {
        try {
            String jsonProperty = (String) field.get(null);
            if (jsonObject.get(jsonProperty) == null) {
                jsonObject.add(jsonProperty, null);
            }
        } catch (IllegalAccessException ex) {
            LOGGER.error("Error preparing spark stream", ex);
        }
    }

    @Override
    public String getTopic() {
        return ActiveMqBrokerManager.SPARK_TOPIC;
    }
}
