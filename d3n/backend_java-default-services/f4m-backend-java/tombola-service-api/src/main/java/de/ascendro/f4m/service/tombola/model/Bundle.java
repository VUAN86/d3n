package de.ascendro.f4m.service.tombola.model;

import java.math.BigDecimal;

import de.ascendro.f4m.service.payment.model.Currency;

public class Bundle {

    protected int amount;
    protected BigDecimal price;
    protected Currency currency;
    protected String imageId;

    public Bundle(int amount, BigDecimal price, Currency currency, String imageId) {
        this.amount = amount;
        this.price = price;
        this.currency = currency;
        this.imageId = imageId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Bundle{");
        sb.append("amount=").append(amount);
        sb.append(", price=").append(price);
        sb.append(", currency=").append(currency);
        sb.append(", imageId='").append(imageId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
