package de.tho.reloading.utility;

import java.math.BigDecimal;

/** Unit a user enters and reads lengths in; the database always stores meters. */
public enum LengthUnit {
    MILLIMETER("Millimeter", "mm", "0.001", 3),
    INCH("Zoll", "in", "0.0254", 4);

    private final String label;
    private final String symbol;
    private final BigDecimal factor;
    private final int displayScale;

    LengthUnit(String label, String symbol, String factor, int displayScale) {
        this.label = label;
        this.symbol = symbol;
        this.factor = new BigDecimal(factor);
        this.displayScale = displayScale;
    }

    public String getLabel() {
        return label;
    }

    public String getSymbol() {
        return symbol;
    }

    /** Value in this unit to the SI value for the database. */
    public BigDecimal toSi(BigDecimal value) {
        return Units.toSi(value, factor);
    }

    /** SI value to this unit with full precision, for input fields. */
    public BigDecimal fromSi(BigDecimal si) {
        return Units.fromSi(si, factor);
    }

    /** SI value to this unit, rounded for display; empty for null. */
    public String format(BigDecimal si) {
        return Units.format(si, factor, displayScale);
    }
}
