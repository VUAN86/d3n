package de.ascendro.f4m.service.payment.payment.system.model;


import javax.persistence.*;

@Entity
@Table(name = "tenant_balance")
@Access(AccessType.FIELD)
@AttributeOverride(name = "id",column = @Column(name = "tenantId"))
public class TenantBalance extends AbstractBalanceEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "enum('active','inactive')")
    private TenantStatus status;

    @Version
    @Column(name ="version")
    private Integer version;

    public TenantBalance() {
    }

    public TenantStatus getStatus() {
        return status;
    }

    public void setStatus(TenantStatus status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }



    @Override
    public String toString() {
        return "TenantBalance{" +
                "status=" + status +
                '}';
    }
}
