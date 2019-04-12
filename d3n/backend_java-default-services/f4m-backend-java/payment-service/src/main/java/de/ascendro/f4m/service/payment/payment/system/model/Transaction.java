package de.ascendro.f4m.service.payment.payment.system.model;

import com.google.api.client.json.Json;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionFilterType;
import de.ascendro.f4m.service.payment.model.external.PaymentTransactionState;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Access(AccessType.FIELD)
@Table(name = "transaction", schema = "nightly_development")
public class Transaction {

    @Id
    @Column(name = "transactionId", nullable = false, length = 255)
    private String transactionId;

    @Version
    @Column(name ="version")
    private Integer version;

    @Column(name = "fromProfileId", nullable = true, length = 255)
    private String fromProfileId;

    @Column(name = "mgiId", nullable = true, length = 255)
    private String mgiId;

    @Column(name = "amount", nullable = false, precision = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", columnDefinition = "enum('BONUS','CREDIT','MONEY')")
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "enum('PENDING', 'PROCESSED')")
    private PaymentTransactionState status;

    @Column(name = "startDate", columnDefinition = "CURRENT_TIMESTAMP")
    private LocalDateTime startDate = LocalDateTime.now();

    @Column(name = "endDate", nullable = true)
    private LocalDateTime endDate;

    @Column(name = "appId", nullable = true)
    private int appId;

    @Column(name = "tenantId", nullable = false)
    private Integer tenantId;

    @Column(name = "description", nullable = true, length = 255)
    private String description;

    @Column(name = "toProfileId", nullable = true, length = 255)
    private String toProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", columnDefinition = "enum('DEBIT','CREDIT','TRANSFER')")
    private TransactionFilterType type;

    @Column(name = "details", nullable = true, length = -1)
    private String details;


    public Transaction() {
    }

    public Transaction(String fromProfileId, String toProfileId, String mgiId, BigDecimal amount, Currency currency,
                       PaymentTransactionState status, int appId, Integer tenantId, String description) {
        this.fromProfileId = fromProfileId;
        this.toProfileId = toProfileId;
        this.mgiId = mgiId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.appId = appId;
        this.tenantId = tenantId;
        this.description = description;
        this.endDate = LocalDateTime.now();
    }


    public Transaction(String transactionId, BigDecimal amount, Currency currency, PaymentTransactionState status,
                       int appId, Integer tenantId, String description) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.appId = appId;
        this.tenantId = tenantId;
        this.description = description;
    }

    public Transaction(String transactionId, String fromProfileId, String mgiId, BigDecimal amount, Currency currency,
                       PaymentTransactionState status, int appId, Integer tenantId, String description, String toProfileId) {
        this.transactionId = transactionId;
        this.fromProfileId = fromProfileId;
        this.mgiId = mgiId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.appId = appId;
        this.tenantId = tenantId;
        this.description = description;
        this.toProfileId = toProfileId;
    }


    public Transaction transactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public Transaction fromProfileId(String fromProfileId) {
        this.fromProfileId = fromProfileId;
        return this;
    }

    public Transaction mgiId(String mgiId) {
        this.mgiId = mgiId;
        return this;
    }

    public Transaction amount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public Transaction currency(Currency currency) {
        this.currency = currency;
        return this;
    }

    public Transaction status(PaymentTransactionState status) {
        this.status = status;
        return this;
    }

    public Transaction appId(Integer appId) {
        this.appId = appId;
        return this;
    }

    public Transaction tenantId(String tenantId) {
        this.tenantId = Integer.parseInt(tenantId);
        return this;
    }

    public Transaction description(String description) {
        this.description = description;
        return this;
    }

    public Transaction toProfileId(String toProfileId) {
        this.toProfileId = toProfileId;
        return this;
    }

    public Transaction type(TransactionFilterType type) {
        this.type = type;
        return this;
    }

    public Transaction details(String details) {
        this.details = details;
        return this;
    }

    public Transaction endDate(LocalDateTime endDate) {
        this.endDate = endDate;
        return this;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getFromProfileId() {
        return fromProfileId;
    }

    public void setFromProfileId(String profileId) {
        this.fromProfileId = profileId;
    }

    public String getMgiId() {
        return mgiId;
    }

    public void setMgiId(String mgiId) {
        this.mgiId = mgiId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentTransactionState getStatus() {
        return status;
    }

    public void setStatus(PaymentTransactionState status) {
        this.status = status;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public Integer getTenantId() {
        return tenantId;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
    }

    public String getToProfileId() {
        return toProfileId;
    }

    public void setToProfileId(String toProfileId) {
        this.toProfileId = toProfileId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public TransactionFilterType getType() {
        return type;
    }

    public void setType(TransactionFilterType type) {
        this.type = type;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return appId == that.appId &&
                transactionId.equals(that.transactionId) &&
                version.equals(that.version) &&
                Objects.equals(fromProfileId, that.fromProfileId) &&
                Objects.equals(mgiId, that.mgiId) &&
                amount.equals(that.amount) &&
                currency == that.currency &&
                startDate.equals(that.startDate) &&
                Objects.equals(endDate, that.endDate) &&
                tenantId.equals(that.tenantId) &&
                Objects.equals(toProfileId, that.toProfileId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, version, fromProfileId, mgiId, amount, currency, startDate, endDate, appId, tenantId, toProfileId);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", version=" + version +
                ", fromProfileId='" + fromProfileId + '\'' +
                ", mgiId='" + mgiId + '\'' +
                ", amount=" + amount +
                ", currency=" + currency +
                ", status=" + status +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", appId=" + appId +
                ", tenantId=" + tenantId +
                ", description='" + description + '\'' +
                ", toProfileId='" + toProfileId + '\'' +
                ", type=" + type +
                ", details='" + details + '\'' +
                '}';
    }
}
