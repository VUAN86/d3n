package de.ascendro.f4m.service.tombola.model;

import java.util.ArrayList;
import java.util.List;

public class UserTombolaInfo {
    private String tombolaId;
    private String name;
    private String drawDate;
    private String imageId;
    private boolean isPending;
    private int totalTicketsBought;
    private int totalPrizesWon;
    private List<PurchasedBundle> bundles;
    private List<TombolaWinner> prizes;

    public UserTombolaInfo() {
        this.tombolaId = "";
        this.name = "";
        this.imageId = "";
        this.drawDate = "";
        this.isPending = false;
        this.totalTicketsBought = 0;
        this.totalPrizesWon = 0;
        this.bundles = new ArrayList<>();
        this.prizes = new ArrayList<>();
    }

    public UserTombolaInfo(String tombolaId, String name, String drawDate, Integer totalTicketsBought,
            Integer totalPrizesWon, List<PurchasedBundle> bundles, List<TombolaWinner> prizes) {
        this.tombolaId = tombolaId;
        this.name = name;
        this.drawDate = drawDate;
        this.totalTicketsBought = totalTicketsBought;
        this.totalPrizesWon = totalPrizesWon;
        this.bundles = bundles;
        this.prizes = prizes;
    }

    public UserTombolaInfo(String tombolaId, String name, String imageId, String drawDate, boolean isPending,
            Integer totalTicketsBought, List<PurchasedBundle> bundles) {
        this.tombolaId = tombolaId;
        this.name = name;
        this.imageId = imageId;
        this.drawDate = drawDate;
        this.isPending = isPending;
        this.totalTicketsBought = totalTicketsBought;
        this.totalPrizesWon = 0;
        this.bundles = bundles;
        this.prizes = new ArrayList<>();
    }

    public String getTombolaId() {
        return tombolaId;
    }

    public void setTombolaId(String tombolaId) {
        this.tombolaId = tombolaId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getDrawDate() {
        return drawDate;
    }

    public void setDrawDate(String drawDate) {
        this.drawDate = drawDate;
    }

    public boolean isPending() {
        return isPending;
    }

    public void setPending(boolean pending) {
        isPending = pending;
    }

    public int getTotalTicketsBought() {
        return totalTicketsBought;
    }

    public void setTotalTicketsBought(int totalTicketsBought) {
        this.totalTicketsBought = totalTicketsBought;
    }

    public int getTotalPrizesWon() {
        return totalPrizesWon;
    }

    public void setTotalPrizesWon(int totalPrizesWon) {
        this.totalPrizesWon = totalPrizesWon;
    }

    public List<PurchasedBundle> getBundles() {
        return bundles;
    }

    public void setBundles(List<PurchasedBundle> bundles) {
        this.bundles = bundles;
    }

    public List<TombolaWinner> getPrizes() {
        return prizes;
    }

    public void setPrizes(List<TombolaWinner> prizes) {
        this.prizes = prizes;
    }

    public void addPrizes(List<TombolaWinner> prizes) {
        this.prizes.addAll(prizes);
    }

    @Override
    public String toString() {
        return new StringBuilder().append("UserTombolaInfo{")
                .append("tombolaId='").append(tombolaId).append('\'')
                .append(", name='").append(name).append('\'')
                .append(", imageId='").append(imageId).append('\'')
                .append(", drawDate='").append(drawDate).append('\'')
                .append(", isPending='").append(isPending).append('\'')
                .append(", totalTicketsBought=").append(totalTicketsBought)
                .append(", totalPrizesWon=").append(totalPrizesWon)
                .append(", bundles=").append(bundles)
                .append(", prizes=").append(prizes)
                .append('}').toString();
    }
}
