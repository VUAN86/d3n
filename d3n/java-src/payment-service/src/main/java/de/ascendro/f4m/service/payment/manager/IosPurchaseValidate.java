package de.ascendro.f4m.service.payment.manager;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ArrayMap;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.model.Ios;
import de.ascendro.f4m.service.payment.model.external.IosPurchase;
import java.util.Map;

public class IosPurchaseValidate {

    public static Ios getReceiptInfo(IosPurchase iosPurchase, PaymentConfig config) throws Exception {
        String URL_TEST = config.getProperty(PaymentConfig.PAYMENT_SYSTEM_ITUNES_TEST);
        String URL_PROD = config.getProperty(PaymentConfig.PAYMENT_SYSTEM_ITUNES);
        Ios response = null;
            response = getValidate(iosPurchase, URL_PROD);
            if (response.getStatus() == 21007) {
                response = getValidate(iosPurchase, URL_TEST);
            }
        return response;
    }

    private static  Ios getValidate(IosPurchase iosPurchase, String URL) throws Exception {
        Map<String, String> requestData = new ArrayMap<>();
        JsonFactory jsonFactory = new JacksonFactory();
        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
        requestData.put("receipt-data", iosPurchase.getReceipt());
        HttpContent httpContent = ByteArrayContent.fromString("application/json", jsonFactory.toPrettyString(requestData));
        HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(URL), httpContent);
        request.getHeaders().setContentType("application/x-www-form-urlencoded");
        request.setParser(jsonFactory.createJsonObjectParser());
        HttpResponse response =request.execute();
        Ios ios = response.parseAs(Ios.class);
        return ios;
    }
}
