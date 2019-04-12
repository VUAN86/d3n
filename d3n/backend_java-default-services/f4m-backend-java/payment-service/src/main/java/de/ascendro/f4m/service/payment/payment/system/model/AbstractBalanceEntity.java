package de.ascendro.f4m.service.payment.payment.system.model;

import javax.persistence.*;
import java.math.BigDecimal;

@MappedSuperclass
@Access(AccessType.FIELD)
public abstract class AbstractBalanceEntity extends AbstractBaseEntity {

    @Column(name = "balance", precision = 2, columnDefinition = "Decimal(15,2) default '0.00'")
    private BigDecimal balance = BigDecimal.ZERO;



    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }


}
