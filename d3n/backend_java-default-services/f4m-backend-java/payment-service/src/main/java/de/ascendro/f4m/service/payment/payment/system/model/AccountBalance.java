package de.ascendro.f4m.service.payment.payment.system.model;

import de.ascendro.f4m.service.payment.model.Currency;

import javax.persistence.*;
import java.util.Objects;


@NamedQueries({
        @NamedQuery(
                name = AccountBalance.GET_USER_ACCOUNT_BALANCE,
                query = "SELECT a FROM AccountBalance a WHERE a.profileId=:profileId AND a.tenant.id=:tenantId")
})
@Entity
@Table(name = "account_balance", schema = "nightly_development")
public class AccountBalance extends AbstractBalanceEntity {

    public static final String GET_USER_ACCOUNT_BALANCE = "AccountBalance.getUserAccountBalance";

    @Column(name = "profileId")
    private String profileId;

    @Version
    @Column(name ="version")
    private Integer version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenantId", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "accountCurrency", columnDefinition = "enum('BONUS','CREDIT','MONEY')")
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "enum('ENABLED', 'DISABLED', 'CONVERTED', 'CLOSED', 'MERGED')")
    private AccountStatus status;

    public AccountBalance() {
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountBalance)) return false;
        if (!super.equals(o)) return false;

        AccountBalance that = (AccountBalance) o;

        if (!getProfileId().equals(that.getProfileId())) return false;
        if (getVersion() != null ? !getVersion().equals(that.getVersion()) : that.getVersion() != null) return false;
        if (!getTenant().equals(that.getTenant())) return false;
        if (getCurrency() != that.getCurrency()) return false;
        return getStatus() == that.getStatus();
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getProfileId().hashCode();
        result = 31 * result + (getVersion() != null ? getVersion().hashCode() : 0);
        result = 31 * result + getTenant().hashCode();
        result = 31 * result + getCurrency().hashCode();
        result = 31 * result + (getStatus() != null ? getStatus().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AccountBalance{" +
                "profileId='" + profileId + '\'' +
                ", version=" + version +
                ", tenant.id=" + tenant.id +
                ", currency=" + currency +
                ", status=" + status +
                '}';
    }
}