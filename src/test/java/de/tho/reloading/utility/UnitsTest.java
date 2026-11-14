package de.tho.reloading.utility;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class UnitsTest {

    @Test
    void shouldConvertGrainsToKilograms() {
        assertThat(WeightUnit.GRAIN.toSi(new BigDecimal("140"))).isEqualByComparingTo("0.0090718474");
        assertThat(WeightUnit.GRAIN.toSi(new BigDecimal("41.5"))).isEqualByComparingTo("0.002689154765");
        assertThat(WeightUnit.GRAM.toSi(new BigDecimal("9.07"))).isEqualByComparingTo("0.00907");
    }

    @Test
    void shouldConvertLengthsToMeters() {
        assertThat(LengthUnit.MILLIMETER.toSi(new BigDecimal("6.71"))).isEqualByComparingTo("0.00671");
        assertThat(LengthUnit.INCH.toSi(new BigDecimal("0.264"))).isEqualByComparingTo("0.0067056");
    }

    @Test
    void shouldReturnTheEnteredValueForInputFields() {
        assertThat(WeightUnit.GRAIN.fromSi(new BigDecimal("0.0090718474")).toPlainString()).isEqualTo("140");
        assertThat(WeightUnit.GRAIN.fromSi(new BigDecimal("0.002689154765")).toPlainString()).isEqualTo("41.5");
        assertThat(LengthUnit.INCH.fromSi(new BigDecimal("0.0067056")).toPlainString()).isEqualTo("0.264");
        assertThat(LengthUnit.MILLIMETER.fromSi(new BigDecimal("0.1")).toPlainString()).isEqualTo("100");
    }

    @Test
    void shouldKeepTheStoredValueWhenEditedInAnotherUnit() {
        BigDecimal stored = new BigDecimal("0.00671"); // entered as 6.71 mm

        BigDecimal inInches = LengthUnit.INCH.fromSi(stored);

        assertThat(LengthUnit.INCH.toSi(inInches)).isEqualByComparingTo(stored);
    }

    @Test
    void shouldFormatRoundedForDisplay() {
        assertThat(WeightUnit.GRAIN.format(new BigDecimal("0.0090718474"))).isEqualTo("140");
        assertThat(WeightUnit.GRAM.format(new BigDecimal("0.0090718474"))).isEqualTo("9.072");
        assertThat(LengthUnit.MILLIMETER.format(new BigDecimal("0.00671"))).isEqualTo("6.71");
        assertThat(LengthUnit.INCH.format(new BigDecimal("0.00671"))).isEqualTo("0.2642");
        assertThat(LengthUnit.INCH.format(null)).isEmpty();
    }

    @Test
    void shouldPassNullThrough() {
        assertThat(WeightUnit.GRAIN.toSi(null)).isNull();
        assertThat(LengthUnit.INCH.fromSi(null)).isNull();
    }
}
