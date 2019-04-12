import communicator.RabbitClient;

public class ServiceStart {
    private static String ROUTING_KEY = "voucher";
    private static String QUEUE_NAME = "voucher";


    public static void main(String... args) throws Exception {
        System.out.println("Service Started");
//        RabbitClient.createCallbackReciever(QUEUE_NAME);
        System.out.println(RabbitClient.recieve(ROUTING_KEY));
//        RabbitClient.send(ROUTING_KEY,"{\"seq\":null,\"ack\":null,\"error\":null,\"message\":\"promocode/promocodeUse\",\"content\":{\"code\":\"12313\"},\"timestamp\":1549953286826,\"clientId\":\"7ea76210-3748-4a2b-9500-409a53f640a6\",\"clientInfo\":{\"profile\":{\"userId\":\"1549282755454d824c0d7-4ba3-4869-85e2-e8b041a4f626\",\"handicap\":0,\"roles\":[\"REGISTERED\"],\"originCountry\":\"RU\",\"emails\":[\"o10958473@nwytg.net\"]},\"appConfig\":{\"tenantId\":\"3\",\"appId\":\"37\",\"deviceUUID\":\"1684684cde4c8ed48c84ced4\"},\"ip\":\"91.234.153.94\",\"countryCode\":\"RU\"},\"token\":null}", false);

    }
}
