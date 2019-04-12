package de.ascendro.f4m.service.payment.manager;

import org.apache.commons.lang3.StringUtils;

public enum IosErrorCode {

    INVALID_JSON("21000", "The App Store could not read the JSON object you provided."),
    INVALID_RECEIPT("21002", "The data in the receipt-data property was malformed."),
    RECEIPT_COULD_NOT_BE_AUTHENTICATED("21003", "The receipt could not be authenticated."),
    INVALID_SHARED_SECRET("21004", "The shared secret you provided does not match the shared secret on file for your account."),
    SERVER_NOT_AVAILABLE("21005", "The receipt server is not currently available."),
    SUBSCRIPTION_HAS_EXPIRED("21006", "This receipt is valid but the subscription has expired. When this status code is returned to your server, the receipt data is also decoded and returned as part of the response."),
    SANDBOX_RECEIPT("21007", "This receipt is a sandbox receipt, but it was sent to the production service for verification."),
    PRODUCTION_RECEIPT("21008", "This receipt is a production receipt, but it was sent to the sandbox service for verification."),
    INVALID;

    private String code;
    private String description;

    private IosErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }
    private IosErrorCode() {
    }
    public String getCode() {
        return code;
    }


    public String getDescription() {
        return description;
    }

    public static IosErrorCode byCode(String code) {
        IosErrorCode externalErrorCode = null;
        if (StringUtils.isNotEmpty(code)) {
            for (IosErrorCode errCode : IosErrorCode.values()) {
                if (errCode.getCode().equals(code)) {
                    externalErrorCode = errCode;
                    break;
                }
            }
            return externalErrorCode;
        } else return IosErrorCode.INVALID;

    }
}
