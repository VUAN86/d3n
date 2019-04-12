package de.ascendro.f4m.service.voucher.model;

/**
 * Created by Alexandru on 12/8/2016.
 */
public enum UserVoucherType {
    CONSUMED("consumed"),
    LOCKED("locked"),
    AVAILABLE("available");

    private final String text;

    private UserVoucherType(String text) {
        this.text = text;
    }

    public static UserVoucherType getByText(String text) {
        return valueOf(text.toUpperCase());
    }

    public String getText() {
        return text;
    }
}
