package de.ascendro.f4m.service.payment.manager;

import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.model.external.AndroidPurchase;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.model.ProductPurchase;

import java.util.Collections;

public class AndroidPurchaseValidate {
    public ProductPurchase getReceiptInfo(AndroidPurchase androidPurchase, PaymentConfig config) throws Exception {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        GoogleCredential credential = GoogleCredential
                .fromStream(this.getClass().getResourceAsStream("/de/f4m/service/payment/manager/api-7332586784272489417-65030-02280e2a8539.json"), httpTransport, jsonFactory);
        credential = credential.createScoped(Collections.singleton(config.getProperty(PaymentConfig.PAYMENT_SYSTEM_PLAYMARKET)));
        AndroidPublisher publisher = new AndroidPublisher.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName(androidPurchase.getPackageName())
                .build();

        AndroidPublisher.Purchases purchases = publisher.purchases();

        final AndroidPublisher.Purchases.Products.Get request = purchases.products().get(androidPurchase.getPackageName(), androidPurchase.getProductId(),
                androidPurchase.getPurchaseToken());
        return request.execute();
    }
}
