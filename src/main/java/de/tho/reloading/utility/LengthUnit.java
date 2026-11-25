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

    /** SI value to this unit with exactly this number of decimal places; empty for null. */
    public String format(BigDecimal si, int decimalPlaces) {
        return Units.formatFixed(si, factor, decimalPlaces);
    }

    /**
     * Bullet diameters are always shown the way they are named: in inches with three decimal places and without
     * the leading zero, followed by millimeters with two, e.g. ".264 in (6.71 mm)". Independent of the units of
     * the user; empty for null.
     */
    public static String formatDiameter(BigDecimal si) {
        if (si == null) {
            return "";
        }
        String inches = INCH.format(si, 3);
        if (inches.startsWith("0.")) {
            inches = inches.substring(1);
        }
        return inches + " " + INCH.symbol + " (" + MILLIMETER.format(si, 2) + " " + MILLIMETER.symbol + ")";
    }

    /**
     * For lengths that are shown in both units whatever the user prefers, e.g. "660 mm (25.98 in)":
     * millimeters with up to three decimal places, inches with up to two. Empty for null.
     */
    public static String formatMillimetersAndInches(BigDecimal si) {
        return formatMillimetersAndInches(si, 2);
    }

    /** Like {@link #formatMillimetersAndInches(BigDecimal)} with this many decimal places for the inches. */
    public static String formatMillimetersAndInches(BigDecimal si, int inchDecimalPlaces) {
        if (si == null) {
            return "";
        }
        String inches = new BigDecimal(INCH.format(si, inchDecimalPlaces)).stripTrailingZeros().toPlainString();
        return MILLIMETER.format(si) + " " + MILLIMETER.symbol + " (" + inches + " " + INCH.symbol + ")";
    }
}
