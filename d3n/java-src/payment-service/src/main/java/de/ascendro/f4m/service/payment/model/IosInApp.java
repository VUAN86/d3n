package de.ascendro.f4m.service.payment.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class IosInApp implements JsonMessageContent {
    @com.google.api.client.util.Key
    private String quantity;
    @com.google.api.client.util.Key
    private String product_id;
    @com.google.api.client.util.Key
    private String transaction_id;
    @com.google.api.client.util.Key
    private String original_transaction_id;
    @com.google.api.client.util.Key
    private String purchase_date;
    @com.google.api.client.util.Key
    private String purchase_date_ms;
    @com.google.api.client.util.Key
    private String purchase_date_pst;
    @com.google.api.client.util.Key
    private String original_purchase_date;
    @com.google.api.client.util.Key
    private String original_purchase_date_ms;
    @com.google.api.client.util.Key
    private String original_purchase_date_pst;
    @com.google.api.client.util.Key
    private String is_trial_period;

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getProduct_id() {
        return product_id;
    }

    public void setProduct_id(String product_id) {
        this.product_id = product_id;
    }

    public String getTransaction_id() {
        return transaction_id;
    }

    public void setTransaction_id(String transaction_id) {
        this.transaction_id = transaction_id;
    }

    public String getOriginal_transaction_id() {
        return original_transaction_id;
    }

    public void setOriginal_transaction_id(String original_transaction_id) {
        this.original_transaction_id = original_transaction_id;
    }

    public String getPurchase_date() {
        return purchase_date;
    }

    public void setPurchase_date(String purchase_date) {
        this.purchase_date = purchase_date;
    }

    public String getPurchase_date_ms() {
        return purchase_date_ms;
    }

    public void setPurchase_date_ms(String purchase_date_ms) {
        this.purchase_date_ms = purchase_date_ms;
    }

    public String getPurchase_date_pst() {
        return purchase_date_pst;
    }

    public void setPurchase_date_pst(String purchase_date_pst) {
        this.purchase_date_pst = purchase_date_pst;
    }

    public String getOriginal_purchase_date() {
        return original_purchase_date;
    }

    public void setOriginal_purchase_date(String original_purchase_date) {
        this.original_purchase_date = original_purchase_date;
    }

    public String getOriginal_purchase_date_ms() {
        return original_purchase_date_ms;
    }

    public void setOriginal_purchase_date_ms(String original_purchase_date_ms) {
        this.original_purchase_date_ms = original_purchase_date_ms;
    }

    public String getOriginal_purchase_date_pst() {
        return original_purchase_date_pst;
    }

    public void setOriginal_purchase_date_pst(String original_purchase_date_pst) {
        this.original_purchase_date_pst = original_purchase_date_pst;
    }

    public String getIs_trial_period() {
        return is_trial_period;
    }

    public void setIs_trial_period(String is_trial_period) {
        this.is_trial_period = is_trial_period;
    }
}