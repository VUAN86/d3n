package de.ascendro.f4m.service.tombola.model;

public class TombolaTicket {

    private int id;
    private String code;
    private String userId;
    private String appId;
    private String obtainDate;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getObtainDate() {
        return obtainDate;
    }

    public void setObtainDate(String obtainDate) {
        this.obtainDate = obtainDate;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("TombolaTicket{")
                .append("code='").append(code).append('\'')
                .append(", userId='").append(userId).append('\'')
                .append(", obtainDate='").append(obtainDate).append('\'')
                .append('}').toString();
    }
}
