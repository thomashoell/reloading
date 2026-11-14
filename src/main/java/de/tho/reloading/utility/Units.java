package de.tho.reloading.utility;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Conversion between the SI base units of the database and the units a user works with.
 * Shared by {@link WeightUnit} and {@link LengthUnit}.
 */
final class Units {

    // SI values are rounded to this scale, so that converting a displayed value back gives the stored value again
    private static final int SI_SCALE = 15;

    private Units() {
    }

    static BigDecimal toSi(BigDecimal value, BigDecimal factor) {
        if (value == null) {
            return null;
        }
        return plain(value.multiply(factor).setScale(SI_SCALE, RoundingMode.HALF_UP));
    }

    static BigDecimal fromSi(BigDecimal si, BigDecimal factor) {
        if (si == null) {
            return null;
        }
        return plain(si.divide(factor, MathContext.DECIMAL64));
    }

    static String format(BigDecimal si, BigDecimal factor, int scale) {
        if (si == null) {
            return "";
        }
        return plain(si.divide(factor, MathContext.DECIMAL64).setScale(scale, RoundingMode.HALF_UP)).toPlainString();
    }

    /** Without trailing zeros and without an exponent (100 instead of 1E+2). */
    private static BigDecimal plain(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
