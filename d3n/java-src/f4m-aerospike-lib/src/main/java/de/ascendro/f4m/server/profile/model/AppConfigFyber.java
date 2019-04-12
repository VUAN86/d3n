package de.ascendro.f4m.server.profile.model;

public class AppConfigFyber {

    //Fyber Settings
    private String clientSecurityToken;
    private String rewardHandlingSecurityToken;
    private String appId;


    public String getClientSecurityToken() {
        return clientSecurityToken;
    }

    public void setClientSecurityToken(String clientSecurityToken) {
        this.clientSecurityToken = clientSecurityToken;
    }

    public String getRewardHandlingSecurityToken() {
        return rewardHandlingSecurityToken;
    }

    public void setRewardHandlingSecurityToken(String rewardHandlingSecurityToken) {
        this.rewardHandlingSecurityToken = rewardHandlingSecurityToken;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    @Override
    public String toString() {
        return "AppConfigFyber{" +
                "clientSecurityToken='" + clientSecurityToken + '\'' +
                ", rewardHandlingSecurityToken='" + rewardHandlingSecurityToken + '\'' +
                ", appId='" + appId + '\'' +
                '}';
    }
}
