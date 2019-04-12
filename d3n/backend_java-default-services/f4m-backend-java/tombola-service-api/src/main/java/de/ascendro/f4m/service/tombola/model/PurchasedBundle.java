package de.ascendro.f4m.service.tombola.model;

import java.math.BigDecimal;

import de.ascendro.f4m.service.payment.model.Currency;

public class PurchasedBundle extends Bundle{
    private String purchaseDate;
    private int firstTicketId;

    public PurchasedBundle(int amount, BigDecimal price, Currency currency, String imageId, int firstTicketId,
            String purchaseDate) {
        super(amount, price, currency, imageId);
        this.firstTicketId = firstTicketId;
        this.purchaseDate = purchaseDate;
    }

    public int getFirstTicketId() {
        return firstTicketId;
    }

    public void setFirstTicketId(int firstTicketId) {
        this.firstTicketId = firstTicketId;
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(String purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PurchasedBundle{");
        sb.append("amount=").append(amount);
        sb.append(", purchaseDate='").append(purchaseDate).append('\'');
        sb.append(", price=").append(price);
        sb.append(", firstTicketId=").append(firstTicketId);
        sb.append(", currency=").append(currency);
        sb.append(", imageId='").append(imageId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
