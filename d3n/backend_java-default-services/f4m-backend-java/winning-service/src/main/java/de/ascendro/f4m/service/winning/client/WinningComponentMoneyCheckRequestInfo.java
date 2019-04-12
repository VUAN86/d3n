package de.ascendro.f4m.service.winning.client;

import de.ascendro.f4m.service.request.RequestInfoImpl;

public class WinningComponentMoneyCheckRequestInfo extends RequestInfoImpl {
    private String userWinningComponentId;
    private String winningComponentId;
    private String winningOptionId;

    public WinningComponentMoneyCheckRequestInfo(String userWinningComponentId, String winningComponentId, String winningOptionId) {
        this.userWinningComponentId = userWinningComponentId;
        this.winningComponentId = winningComponentId;
        this.winningOptionId = winningOptionId;
    }

    public String getUserWinningComponentId() {
        return userWinningComponentId;
    }

    public void setUserWinningComponentId(String userWinningComponentId) {
        this.userWinningComponentId = userWinningComponentId;
    }

    public String getWinningComponentId() {
        return winningComponentId;
    }

    public void setWinningComponentId(String winningComponentId) {
        this.winningComponentId = winningComponentId;
    }

    public String getWinningOptionId() {
        return winningOptionId;
    }

    public void setWinningOptionId(String winningOptionId) {
        this.winningOptionId = winningOptionId;
    }
}
