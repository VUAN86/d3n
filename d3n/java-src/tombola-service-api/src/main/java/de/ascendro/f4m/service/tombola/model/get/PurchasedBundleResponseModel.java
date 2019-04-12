package de.ascendro.f4m.service.tombola.model.get;

import de.ascendro.f4m.service.tombola.model.Bundle;
import de.ascendro.f4m.service.tombola.model.PurchasedBundle;

public class PurchasedBundleResponseModel extends Bundle{
    private String purchaseDate;

    public PurchasedBundleResponseModel(PurchasedBundle purchasedBundle) {
        super(purchasedBundle.getAmount(), purchasedBundle.getPrice(), purchasedBundle.getCurrency(),
                purchasedBundle.getImageId());
        this.purchaseDate = purchasedBundle.getPurchaseDate();
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(String purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PurchasedBundleResponseModel{");
        sb.append("amount=").append(amount);
        sb.append(", price=").append(price);
        sb.append(", currency=").append(currency);
        sb.append(", imageId='").append(imageId).append('\'');
        sb.append(", purchaseDate='").append(purchaseDate).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
