package de.ascendro.f4m.service.tombola.model;

import java.util.List;

public class Tombola {

    private String id;
    private String name;
    private String description;
    private String winningRules;
    private String imageId;
    private String startDate;
    private String endDate;
    private TombolaStatus status;
    private PlayoutTarget playoutTarget;
    private String targetDate;
    private int percentOfTicketsAmount;
    private List<String> applicationsIds;
    private List<String> regionalSettingsIds;
    private int totalTicketsAmount;
    private int purchasedTicketsAmount;
    private List<Bundle> bundles;
    private List<Prize> prizes;
    private Prize consolationPrize;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWinningRules() {
        return winningRules;
    }

    public void setWinningRules(String winningRules) {
        this.winningRules = winningRules;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public TombolaStatus getStatus() {
        return status;
    }

    public void setStatus(TombolaStatus status) {
        this.status = status;
    }

    public PlayoutTarget getPlayoutTarget() {
        return playoutTarget;
    }

    public void setPlayoutTarget(PlayoutTarget playoutTarget) {
        this.playoutTarget = playoutTarget;
    }

    public String getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(String targetDate) {
        this.targetDate = targetDate;
    }

    public int getPercentOfTicketsAmount() {
        return percentOfTicketsAmount;
    }

    public void setPercentOfTicketsAmount(int percentOfTicketsAmount) {
        this.percentOfTicketsAmount = percentOfTicketsAmount;
    }

    public List<String> getApplicationsIds() {
        return applicationsIds;
    }

    public void setApplicationsIds(List<String> applicationsIds) {
        this.applicationsIds = applicationsIds;
    }

    public List<String> getRegionalSettingsIds() {
        return regionalSettingsIds;
    }

    public void setRegionalSettingsIds(List<String> regionalSettingsIds) {
        this.regionalSettingsIds = regionalSettingsIds;
    }

    public int getTotalTicketsAmount() {
        return totalTicketsAmount;
    }

    public void setTotalTicketsAmount(int totalTicketsAmount) {
        this.totalTicketsAmount = totalTicketsAmount;
    }

    public List<Bundle> getBundles() {
        return bundles;
    }

    public void setBundles(List<Bundle> bundles) {
        this.bundles = bundles;
    }

    public List<Prize> getPrizes() {
        return prizes;
    }

    public void setPrizes(List<Prize> prizes) {
        this.prizes = prizes;
    }

    public int getPurchasedTicketsAmount() {
        return purchasedTicketsAmount;
    }

    public void setPurchasedTicketsAmount(int purchasedTicketsAmount) {
        this.purchasedTicketsAmount = purchasedTicketsAmount;
    }

    public Prize getConsolationPrize() {
        return consolationPrize;
    }

    public void setConsolationPrize(Prize consolationPrize) {
        this.consolationPrize = consolationPrize;
    }

    public boolean isInfinityTickets() {return getTotalTicketsAmount() == -1;}

    @Override
    public String toString() {
        return new StringBuilder().append("Tombola{")
                .append("id='").append(id).append('\'')
                .append(", name='").append(name).append('\'')
                .append(", description='").append(description).append('\'')
                .append(", winningRules='").append(winningRules).append('\'')
                .append(", imageId='").append(imageId).append('\'')
                .append(", startDate='").append(startDate).append('\'')
                .append(", endDate='").append(endDate).append('\'')
                .append(", status=").append(status)
                .append(", playoutTarget=").append(playoutTarget)
                .append(", targetDate='").append(targetDate).append('\'')
                .append(", percentOfTicketsAmount=").append(percentOfTicketsAmount)
                .append(", applicationsIds=").append(applicationsIds)
                .append(", regionalSettingsIds=").append(regionalSettingsIds)
                .append(", totalTicketsAmount=").append(totalTicketsAmount)
                .append(", purchasedTicketsAmount=").append(purchasedTicketsAmount)
                .append(", bundles=").append(bundles)
                .append(", prizes=").append(prizes)
                .append(", consolationPrize=").append(consolationPrize)
                .append('}').toString();
    }
}


