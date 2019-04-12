package de.ascendro.f4m.service.payment.model;

import de.ascendro.f4m.service.json.model.user.ClientInfo;

public class ErrorMobilePurchase {

    private String errorMessage;

    private String receipt;

    private ClientInfo clientInfo;

    private Long timestamp;

    public ErrorMobilePurchase(String errorMessage, String receipt, ClientInfo clientInfo, Long timestamp) {
        this.errorMessage = errorMessage;
        this.receipt = receipt;
        this.clientInfo = clientInfo;
        this.timestamp = timestamp;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ErrorMobilePurchase{" +
                "errorMessage='" + errorMessage + '\'' +
                ", receipt='" + receipt + '\'' +
                ", clientInfo=" + clientInfo +
                ", timestamp=" + timestamp +
                '}';
    }
}
