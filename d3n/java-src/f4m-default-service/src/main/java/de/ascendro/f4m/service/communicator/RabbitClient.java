package de.ascendro.f4m.service.communicator;

import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import de.ascendro.f4m.service.handler.MessageHandler;
import de.ascendro.f4m.service.json.model.MessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitClient {


    private final Logger LOGGER = LoggerFactory.getLogger(RabbitClient.class);

    String serviceName;
    MessageHandler messageHandler;

    public RabbitClient(String serviceName, MessageHandler messageHandler) {
        this.serviceName = serviceName;
        this.messageHandler = messageHandler;
    }

    public void recieve(boolean isClient) throws IOException, TimeoutException {
        String casllbackQueue = serviceName;
        if (isClient) {
            casllbackQueue = serviceName + "Callback" + System.nanoTime();
            RabbitClientSender.setCallbackQueue(casllbackQueue);
        }
        recieveMessages(casllbackQueue, isClient);
    }

    private void recieveMessages(String queueName, boolean isCallback) throws IOException, TimeoutException {

        Connection connection = RabbitClientConfig.factory.newConnection();
        Channel channel = connection.createChannel();

        try {
            channel.queueDeclare(queueName, true, isCallback, false, null);
            System.out.println("Start waiting for messages: {}" + queueName);
            LOGGER.debug("Start waiting for messages: {}", queueName);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                try {
                    String originalMessage = new String(delivery.getBody(), "UTF-8");
                    BasicProperties props = delivery.getProperties();
                    System.out.println("RECIEVED from " + props.getReplyTo() + "{}" + originalMessage);
                    LOGGER.debug(" RECIEVED {} from {}", originalMessage, props.getReplyTo());
                    MessageSource messageSource = new MessageSource(props.getReplyTo());
                        new MessageThread(messageHandler.createServiceMessageHandler(), originalMessage, messageSource).start();

                } catch (Exception e) {
                    LOGGER.error(" mq deliverCallback error {}", e.getMessage());
                    e.printStackTrace();
                }

            };
            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
            });
        } catch (Exception e) {
            LOGGER.error(" mq recieve error {}", e.getMessage());
            e.printStackTrace();
        }
    }

}
