package de.ascendro.f4m.service.tombola.model.get;

import java.util.ArrayList;
import java.util.List;

import de.ascendro.f4m.service.tombola.model.TombolaWinner;
import de.ascendro.f4m.service.tombola.model.UserTombolaInfo;

public class UserTombolaInfoResponseModel {
    private String tombolaId;
    private String name;
    private String imageId;
    private String drawDate;
    private boolean isPending;
    private int totalTicketsBought;
    private int totalPrizesWon;
    private List<PurchasedBundleResponseModel> bundles;
    private List<TombolaWinner> prizes;


    public UserTombolaInfoResponseModel(UserTombolaInfo userTombolaInfo) {
        this.tombolaId = userTombolaInfo.getTombolaId();
        this.name = userTombolaInfo.getName();
        this.imageId = userTombolaInfo.getImageId();
        this.drawDate = userTombolaInfo.getDrawDate();
        this.isPending = userTombolaInfo.isPending();
        this.totalTicketsBought = userTombolaInfo.getTotalTicketsBought();
        this.totalPrizesWon = userTombolaInfo.getTotalPrizesWon();
        this.bundles = new ArrayList<>();
        if (userTombolaInfo.getBundles() != null) {
            userTombolaInfo.getBundles().forEach(purchasedBundle ->
                    this.bundles.add(new PurchasedBundleResponseModel(purchasedBundle)));
        }
        this.prizes = userTombolaInfo.getPrizes();
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

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getImageId() {

        return imageId;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<PurchasedBundleResponseModel> getBundles() {
        return bundles;
    }

    public void setBundles(List<PurchasedBundleResponseModel> bundles) {
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
