package com.itzetech.dpdms.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Whole-franc (RWF) arithmetic. Amounts are plain {@code long}s; only quantities and tax rates have decimals. */
public final class Money {

    private Money() {
    }

    /** {@code 1,500,000 RWF} */
    public static String format(long amount) {
        return String.format(Locale.US, "%,d RWF", amount);
    }

    /** quantity × unit price, rounded half up to a whole franc. */
    public static long lineTotal(BigDecimal quantity, long unitPrice) {
        return quantity.multiply(BigDecimal.valueOf(unitPrice)).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /** subtotal × rate ÷ 100, rounded half up to a whole franc. */
    public static long tax(long subtotal, BigDecimal ratePercent) {
        return BigDecimal.valueOf(subtotal).multiply(ratePercent).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).longValueExact();
    }
}
