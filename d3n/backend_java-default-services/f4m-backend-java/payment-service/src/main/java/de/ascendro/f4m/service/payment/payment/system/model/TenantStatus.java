package de.ascendro.f4m.service.payment.payment.system.model;

public enum TenantStatus {

    active("active"),
    inactive("inactive");

    private String description;

    TenantStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
