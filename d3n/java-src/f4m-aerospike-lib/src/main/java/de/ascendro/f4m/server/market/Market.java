package de.ascendro.f4m.server.market;

import java.math.BigDecimal;

public enum Market {

    TEST_PACKAGE_0001("test_package_0001", BigDecimal.valueOf(10)),
    TEST_PACKAGE_0002("test_package_0002", BigDecimal.valueOf(20)),
    TEST_PACKAGE_0003("test_package_0003", BigDecimal.valueOf(50)),
    TEST_PACKAGE_0004("test_package_0004", BigDecimal.valueOf(120)),
    TEST_PACKAGE_0005("test_package_0005", BigDecimal.valueOf(250)),
    TEST_PACKAGE_0006("test_package_0006", BigDecimal.valueOf(600));

    private final String fullName;

    private final BigDecimal value;

    Market(String fullName, BigDecimal value) {
        this.fullName = fullName;
        this.value = value;
    }

    public String getFullName() {
        return fullName;
    }

    public BigDecimal getValue() {
        return value;
    }

}
