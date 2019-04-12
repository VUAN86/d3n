package de.ascendro.f4m.service.tombola.model;

import java.math.BigDecimal;

public class TombolaBuyer {

    private String userId;
    private int purchasedTickets;
    private BigDecimal purchasePrice;
    private int numberOfPrizesWon;

    public TombolaBuyer() {}

    public TombolaBuyer(String userId, int purchasedTickets, BigDecimal purchasePrice, int numberOfPrizesWon) {
        this.userId = userId;
        this.purchasedTickets = purchasedTickets;
        this.purchasePrice = purchasePrice;
        this.numberOfPrizesWon = numberOfPrizesWon;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getPurchasedTickets() {
        return purchasedTickets;
    }

    public void setPurchasedTickets(int purchasedTickets) {
        this.purchasedTickets = purchasedTickets;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public int getNumberOfPrizesWon() {
        return numberOfPrizesWon;
    }

    public void setNumberOfPrizesWon(int numberOfPrizesWon) {
        this.numberOfPrizesWon = numberOfPrizesWon;
    }
}
