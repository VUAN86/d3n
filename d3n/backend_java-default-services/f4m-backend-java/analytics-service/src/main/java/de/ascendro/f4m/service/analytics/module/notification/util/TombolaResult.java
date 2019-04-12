package de.ascendro.f4m.service.analytics.module.notification.util;

import java.util.ArrayList;
import java.util.List;

public class TombolaResult {
    private String userId;
    private boolean isWinner;
    private List<String> prizeNames;

    public TombolaResult(String userId, boolean isWinner) {
        this.userId = userId;
        this.isWinner = isWinner;
        this.prizeNames = new ArrayList<>();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getPrizeNames() {
        return prizeNames;
    }

    public void setPrizeNames(List<String> prizeNames) {
        this.prizeNames = prizeNames;
    }

    public void addPrizeName(String prizeName) {
        prizeNames.add(prizeName);
    }

    public boolean isWinner() {
        return isWinner;
    }

    public void setWinner(boolean winner) {
        isWinner = winner;
    }
}
