package de.ascendro.f4m.service.payment.model.external;

public enum MobileDeviceType {

    IOS("ios"),
    ANDROID("android");

    private final String fullName;

    private MobileDeviceType(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }
}
