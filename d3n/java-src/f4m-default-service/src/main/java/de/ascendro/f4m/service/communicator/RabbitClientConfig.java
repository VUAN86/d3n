package de.ascendro.f4m.service.communicator;

import com.rabbitmq.client.ConnectionFactory;
import de.ascendro.f4m.service.config.F4MConfig;
import de.ascendro.f4m.service.config.F4MConfigImpl;

public class RabbitClientConfig {
    public static ConnectionFactory factory;
    public static String exchangeName;

    private static final F4MConfig config;

    static {

        config = new F4MConfigImpl();
        exchangeName = config.getProperty(F4MConfigImpl.RABBIT_EXCHANGE_NAME);
        factory = new ConnectionFactory();
        factory.setUsername(config.getProperty(F4MConfigImpl.RABBIT_USERNAME));
        factory.setPassword(config.getProperty(F4MConfigImpl.RABBIT_PASSWORD));
        factory.setVirtualHost(config.getProperty(F4MConfigImpl.RABBIT_HOST_VIRTUAL));
        factory.setHost(config.getProperty(F4MConfigImpl.RABBIT_HOST));
        factory.setPort(config.getPropertyAsInteger(F4MConfigImpl.RABBIT_PORT));
        System.out.println("exchangeName = " + exchangeName);
    }
}
