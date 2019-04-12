package de.ascendro.f4m.service.payment.model.external;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class MobilePurchaseRequest implements JsonMessageContent {
    private String receipt;
    private MobileDeviceType device;

    public MobileDeviceType getDevice() {
        return device;
    }

    public void setDevice(MobileDeviceType device) {
        this.device = device;
    }


    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }

}
