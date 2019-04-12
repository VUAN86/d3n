package de.ascendro.f4m.service.advertisement.callback;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.service.payment.model.Currency;

public class FyberParameters {

    public static final String PARAM_TRANSACTION_ID = "_trans_id_";
    public static final String PARAM_USER_ID = "uid";
    public static final String PARAM_SIGNATURE_ID = "sid";
    public static final String PARAM_AMOUNT = "amount";
    public static final String PARAM_CURRENCY_NAME = "currency_name";
    public static final String PARAM_CURRENCY_ID = "currency_id";

    public enum ExtraParameters {
        PUB0("pub0"),
        PUB1("pub1"),
        PUB2("pub2"),
        PUB3("pub3"),
        PUB4("pub4"),
        PUB5("pub5"),
        PUB6("pub6"),
        PUB7("pub7"),
        PUB8("pub8"),
        PUB9("pub9");

        private final String paramName;

        ExtraParameters(String paramName) {
            this.paramName = paramName;
        }

        public String getParamName() {
            return paramName;
        }
    }

    private String transactionId;
    private String userId;
    private String signature;
    private String amount;
    private String currencyName;
    private String currencyId;

    private EnumMap<ExtraParameters, String> customParameters = new EnumMap<>(ExtraParameters.class);

    public void addParameter(ExtraParameters extraParmaeters, String value){
        customParameters.put(extraParmaeters, value);
    }

    public Map<ExtraParameters, String> getCustomParameters() {
        return customParameters;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getSignature() {
        return signature;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        if (StringUtils.isNotBlank(amount)) {
            return new BigDecimal(amount);
        }
        return BigDecimal.ZERO;
    }

    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }

    public void setCurrencyId(String currencyId) {
        this.currencyId = currencyId;
    }

    public Currency getCurrency(){
        return EnumUtils.getEnum(Currency.class, currencyId);
    }

    public String getApplicationId(){
        return customParameters.get(ExtraParameters.PUB0);
    }

    public String getTenantId(){
        return customParameters.get(ExtraParameters.PUB1);
    }

    @Override
    public String toString() {
        StringBuilder customParams = new StringBuilder();
        customParameters.entrySet().forEach(entry ->customParams.append(entry.getKey().getParamName())
                .append("=").append(entry.getValue()));

        return "FyberParameters{" +
                "transactionId='" + transactionId + '\'' +
                ", userId=" + userId +
                ", signature=" + signature +
                ", amount=" + amount +
                ", currencyName=" + currencyName +
                ", currencyId=" + currencyId +
                customParams.toString() +
                '}';
    }

    public boolean hasIdentificationParamas(){
        return getApplicationId() != null && getUserId() != null && getTenantId() !=null;
    }
}
