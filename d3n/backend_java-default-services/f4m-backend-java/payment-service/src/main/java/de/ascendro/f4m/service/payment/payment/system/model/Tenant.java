package de.ascendro.f4m.service.payment.payment.system.model;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "tenant")
public class Tenant extends AbstractBaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "autoCommunity", nullable = false)
    private int autoCommunity;

    @Column(name = "logoUrl", nullable = true, length = 255)
    private String logoUrl;

    @Column(name = "createDate", nullable = false)
    private Timestamp createDate;

    @Column(name = "updateDate", nullable = false)
    private Timestamp updateDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "enum('active','inactive')")
    private TenantStatus status;

    @Column(name = "address", nullable = true, length = 255)
    private String address;

    @Column(name = "city", nullable = true, length = 255)
    private String city;

    @Column(name = "country", nullable = true, length = 255)
    private String country;

    @Column(name = "vat", nullable = true, length = 255)
    private String vat;

    @Column(name = "url", nullable = true, length = 255)
    private String url;

    @Column(name = "email", nullable = true, length = 255)
    private String email;

    @Column(name = "phone", nullable = true, length = 255)
    private String phone;

    @Column(name = "description", nullable = true)
    private String description;

    @Column(name = "currency", nullable = true, length = 3)
    private String currency;

    @Column(name = "contactFirstName", nullable = true, length = 255)
    private String contactFirstName;

    @Column(name = "contactLastName", nullable = true, length = 255)
    private String contactLastName;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "tenant")
    private List<AccountBalance> accountBalances;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAutoCommunity() {
        return autoCommunity;
    }

    public void setAutoCommunity(int autoCommunity) {
        this.autoCommunity = autoCommunity;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Timestamp updateDate) {
        this.updateDate = updateDate;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public void setStatus(TenantStatus status) {
        this.status = status;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getVat() {
        return vat;
    }

    public void setVat(String vat) {
        this.vat = vat;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getContactFirstName() {
        return contactFirstName;
    }

    public void setContactFirstName(String contactFirstName) {
        this.contactFirstName = contactFirstName;
    }

    public String getContactLastName() {
        return contactLastName;
    }

    public void setContactLastName(String contactLastName) {
        this.contactLastName = contactLastName;
    }

    public List<AccountBalance> getAccountBalances() {
        return accountBalances;
    }

    public void setAccountBalances(List<AccountBalance> accountBalances) {
        this.accountBalances = accountBalances;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Tenant tenant = (Tenant) o;
        return autoCommunity == tenant.autoCommunity &&
                Objects.equals(name, tenant.name) &&
                Objects.equals(createDate, tenant.createDate) &&
                Objects.equals(updateDate, tenant.updateDate) &&
                status == tenant.status &&
                Objects.equals(address, tenant.address) &&
                Objects.equals(city, tenant.city) &&
                Objects.equals(country, tenant.country) &&
                Objects.equals(vat, tenant.vat) &&
                Objects.equals(url, tenant.url) &&
                Objects.equals(email, tenant.email) &&
                Objects.equals(phone, tenant.phone) &&
                Objects.equals(description, tenant.description) &&
                Objects.equals(currency, tenant.currency) &&
                Objects.equals(contactFirstName, tenant.contactFirstName) &&
                Objects.equals(contactLastName, tenant.contactLastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, autoCommunity, createDate, updateDate, status, address, city, country, vat, url, email, phone, description, currency, contactFirstName, contactLastName);
    }

    @Override
    public String toString() {
        return "Tenant{" +
                "name='" + name + '\'' +
                ", autoCommunity=" + autoCommunity +
                ", logoUrl='" + logoUrl + '\'' +
                ", createDate=" + createDate +
                ", updateDate=" + updateDate +
                ", status=" + status +
                ", address='" + address + '\'' +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                ", vat='" + vat + '\'' +
                ", url='" + url + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", description='" + description + '\'' +
                ", currency='" + currency + '\'' +
                ", contactFirstName='" + contactFirstName + '\'' +
                ", contactLastName='" + contactLastName + '\'' +
                ", id=" + id +
                '}';
    }
}
