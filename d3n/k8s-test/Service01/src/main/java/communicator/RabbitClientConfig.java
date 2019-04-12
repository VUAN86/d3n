package communicator;

import com.rabbitmq.client.ConnectionFactory;

public class RabbitClientConfig {
    public static ConnectionFactory factory;
    public static String exchangeName = "k8s";
    static {
        factory = new ConnectionFactory();
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setVirtualHost("/");
        factory.setHost("localhost");
        factory.setPort(5672);
    }
}
