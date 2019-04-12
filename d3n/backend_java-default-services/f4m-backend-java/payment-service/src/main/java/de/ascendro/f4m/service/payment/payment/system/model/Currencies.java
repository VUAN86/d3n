package de.ascendro.f4m.service.payment.payment.system.model;

import de.ascendro.f4m.service.payment.model.Currency;

import javax.persistence.*;

@NamedQueries({
        @NamedQuery(
                name = Currencies.GET_ALL_CURRENCIES,
                query = "SELECT c FROM Currencies c")
})
@Entity
@Table(name = "currencies")
public class Currencies extends AbstractBaseEntity {

    public static final String GET_ALL_CURRENCIES = "Currencies.getAllCurrencies";

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", columnDefinition = "enum('BONUS','CREDIT','MONEY')")
    private Currency currency;

    @Column(name = "virtual")
    private Boolean virtual;


    public Currencies() {
    }

    public Currencies(Currency currency, Boolean virtual) {
        this.currency = currency;
        this.virtual = virtual;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public Boolean getVirtual() {
        return virtual;
    }

    public void setVirtual(Boolean virtual) {
        this.virtual = virtual;
    }


    @Override
    public String toString() {
        return "Currencies{" +
                "currency=" + currency +
                ", virtual=" + virtual +
                '}';
    }
}