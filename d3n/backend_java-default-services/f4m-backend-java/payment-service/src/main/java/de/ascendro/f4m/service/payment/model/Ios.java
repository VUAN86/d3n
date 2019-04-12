package de.ascendro.f4m.service.payment.model;

public class Ios extends com.google.api.client.json.GenericJson{

    @com.google.api.client.util.Key
    private java.lang.Integer status;

    @com.google.api.client.util.Key
    private java.lang.String environment;

    @com.google.api.client.util.Key
    private AppleReceipt receipt;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public AppleReceipt getReceipt() {
        return receipt;
    }

    public void setReceipt(AppleReceipt receipt) {
        this.receipt = receipt;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
