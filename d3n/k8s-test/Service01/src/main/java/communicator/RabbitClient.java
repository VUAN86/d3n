package communicator;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class RabbitClient {

    static String casllbackQueue;

    public  static void createCallbackReciever(String queueName) throws IOException, TimeoutException{
        Connection connection = RabbitClientConfig.factory.newConnection();
        Channel channel = connection.createChannel();

        try {
            casllbackQueue=queueName+"Callback"+System.nanoTime();
//            AMQP.Exchange.DeclareOk exchange = channel.exchangeDeclare(casllbackQueue,BuiltinExchangeType.DIRECT, false, true, null);
//            channel.exchangeBind(null,casllbackQueue,queueName);
          casllbackQueue =   channel.queueDeclare(queueName+"Calback"+System.nanoTime(),true,true,false,null ).getQueue();
            System.out.println("casllbackQueue = " + casllbackQueue);
//            channel.queueDeclare(casllbackQueue, true, false, false, null);
            System.out.println("Start waiting for callback messages: " + casllbackQueue);
            AtomicInteger i= new AtomicInteger(1);
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println(delivery.getProperties().getHeaders());
                System.out.println(" RECIEVED callback '" + message + "'");
                MessageWorker mw = new MessageWorker("tata"+ i.getAndIncrement(), new MessageWorkerImpl());
                mw.start();
                Map<String, Object> headers = delivery.getProperties().getHeaders();
                String source = headers.get("source").toString();
                System.out.println(source);

            };
            channel.basicConsume(casllbackQueue, true, deliverCallback, consumerTag -> {
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static String recieve(String queueName) throws IOException, TimeoutException {
        Connection connection = RabbitClientConfig.factory.newConnection();
        Channel channel = connection.createChannel();

        try {
            channel.queueDeclare(queueName, true, false, false, null);
            System.out.println("Start waiting for messages: " + queueName);
            AtomicInteger i= new AtomicInteger(1);
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
//                System.out.println(delivery.getProperties().getHeaders());
                System.out.println(" RECIEVED '" + message + "'");
                MessageWorker mw = new MessageWorker("tata"+ i.getAndIncrement(), new MessageWorkerImpl());
                mw.start();
//                Map<String, Object> headers = delivery.getProperties().getHeaders();
//                String source = headers.get("source").toString();
//                System.out.println(source);

//                try {
//                    RabbitClient.send(delivery.getProperties().getReplyTo(),"fff", true);
//                } catch (TimeoutException e) {
//                    e.printStackTrace();
//                }

            };
            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
            });
            return "1";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String send(String routingKey, String messageBody, boolean isResponse) throws IOException, TimeoutException {
        Connection conn = RabbitClientConfig.factory.newConnection();
        Channel channel = conn.createChannel();

        byte[] messageBodyBytes = (messageBody).getBytes();

        Map<String ,Object> heads = new HashMap<String, Object>();
        heads.put("source","gateway");


        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .headers(heads)
                .replyTo(casllbackQueue)
                .contentType("text/plain")
                .build();
        String exchange =isResponse ? "" : RabbitClientConfig.exchangeName;
        channel.basicPublish(exchange, routingKey, props, messageBodyBytes);
        System.out.println("SEND to "+ routingKey );

        channel.close();
        conn.close();

        return "";
       }
// public static void recieve(String queueName) throws IOException, TimeoutException {
//        Connection connection = RabbitClientConfig.factory.newConnection();
//        Channel channel = connection.createChannel();
//
//        try {
//            channel.queueDeclare(queueName, true, false, false, null);
//            System.out.println("Start waiting for messages: " + queueName);
//            AtomicInteger i= new AtomicInteger(1);
//            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
//                System.out.println("consumerTag = " + consumerTag);
//                String message = new String(delivery.getBody(), "UTF-8");
//                System.out.println(delivery.getProperties().getHeaders());
//                System.out.println(" RECIEVED '" + message + "'");
//                MessageWorker mw = new MessageWorker("tata"+ i.getAndIncrement(), new MessageWorkerImpl());
//                mw.start();
//                Map<String, Object> headers = delivery.getProperties().getHeaders();
//                String source = headers.get("source").toString();
//                System.out.println(source);
//
//
//
//            };
//            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
//            });
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static String send(String routingKey, String messageBody) throws IOException, TimeoutException {
//        Connection conn = RabbitClientConfig.factory.newConnection();
//        Channel channel = conn.createChannel();
//
//
//        byte[] messageBodyBytes = (messageBody).getBytes();
//
//        Map<String ,Object> heads = new HashMap<String, Object>();
//        heads.put("source","gateway");
//
//
//        channel.basicPublish(RabbitClientConfig.exchangeName, routingKey, new AMQP.BasicProperties.Builder()
//                .headers(heads)
//                .contentType("text/plain")
//                .build(), messageBodyBytes);
//        System.out.println("SEND" + messageBodyBytes);
//
//        channel.close();
//        conn.close();
//
//        return "";
//    }
}
