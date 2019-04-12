package de.ascendro.f4m.service.analytics.client;

import de.ascendro.f4m.service.payment.model.Currency;

public class MonthlyInvitesPayoutRequestInfo extends PaymentTransferRequestInfo {

    private Integer targetInvites;

    public Integer getTargetInvites() {
        return targetInvites;
    }

    public void setTargetInvites(Integer targetInvites) {
        this.targetInvites = targetInvites;
    }

    public void setCurrency(String currency) {
        if (currency!=null) {
            if (Currency.BONUS.name().equalsIgnoreCase(currency)) {
                setCurrency(Currency.BONUS);
            } else if (Currency.CREDIT.name().equalsIgnoreCase(currency)) {
                setCurrency(Currency.CREDIT);
            }
        }
    }
}
