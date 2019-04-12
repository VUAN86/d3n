package de.ascendro.f4m.server.analytics.model;


import java.math.BigDecimal;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.analytics.exception.F4MAnalyticsFatalErrorException;
import de.ascendro.f4m.server.analytics.model.base.GameBaseEvent;

public class PromoCodeEvent extends GameBaseEvent {

    public static final String PROMO_CODE = "promoCode";
    public static final String PROMO_CODE_CAMPAIGN_ID_PROPERTY = "promoCodeCampaignId";
    public static final String PROMO_CODE_USED_PROPERTY = "promoCodeUsed";
    public static final String BONUS_POINTS_PAID_PROPERTY = "bonusPointsPaid";
    public static final String CREDITS_PAID_PROPERTY = "creditsPaid";
    public static final String MONEY_PAID_PROPERTY = "moneyPaid";

    public PromoCodeEvent() {
        //default constructor
    }

    public PromoCodeEvent(JsonObject promoCodeJsonObject) {
        super(promoCodeJsonObject);
    }

    public void setPromoCode(String promoCode) {
        setProperty(PROMO_CODE, promoCode);
    }

    public String getPromoCode() {
        return getPropertyAsString(PROMO_CODE);
    }

    public void setPromoCodeCampaignId(Long promoCodeCampaignId) {
        setProperty(PROMO_CODE_CAMPAIGN_ID_PROPERTY, promoCodeCampaignId);
    }

    public Long getPromoCodeCampaignId() {
        return getPropertyAsLong(PROMO_CODE_CAMPAIGN_ID_PROPERTY);
    }

    public void setPromoCodeCampaignId(String promoCodeCampaignId) {
        try {
            setPromoCodeCampaignId(Long.valueOf(promoCodeCampaignId));
        } catch (NumberFormatException ex) {
            throw new F4MAnalyticsFatalErrorException("Invalid PromoCode campaign id");
        }
    }

    public void setPromoCodeUsed(Boolean promoCodeUsed) {
        setProperty(PROMO_CODE_USED_PROPERTY, promoCodeUsed);
    }

    public Boolean isPromoCodeUsed() {
        return getPropertyAsBoolean(PROMO_CODE_USED_PROPERTY);
    }

    public void setCreditsPaid(Long creditsPaid) {
        setProperty(CREDITS_PAID_PROPERTY, creditsPaid);
    }

    public Long getCreditsPaid() {
        return getPropertyAsLong(CREDITS_PAID_PROPERTY);
    }

    public void setBonusPointsPaid(Long bonusPoints) {
        setProperty(BONUS_POINTS_PAID_PROPERTY, bonusPoints);
    }

    public Long getBonusPointsPaid() {
        return getPropertyAsLong(BONUS_POINTS_PAID_PROPERTY);
    }

    public void setMoneyPaid(BigDecimal moneyPaid) {
        setProperty(MONEY_PAID_PROPERTY, moneyPaid);
    }

    public BigDecimal getMoneyPaid() {
        return getPropertyAsBigDecimal(MONEY_PAID_PROPERTY);
    }

}
