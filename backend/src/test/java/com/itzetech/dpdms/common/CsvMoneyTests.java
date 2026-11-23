package com.itzetech.dpdms.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Small checks that need no database: spreadsheet-safe CSV cells and franc rounding. */
class CsvMoneyTests {

    @Test
    void csvCellsAreQuotedAndFormulasAreDefused() {
        assertThat(Csv.cell("Acme Hotels")).isEqualTo("Acme Hotels");
        assertThat(Csv.cell("Acme, Ltd")).isEqualTo("\"Acme, Ltd\"");
        assertThat(Csv.cell("The \"best\" site")).isEqualTo("\"The \"\"best\"\" site\"");
        assertThat(Csv.cell("=HYPERLINK(\"x\")")).startsWith("\"'=");
        assertThat(Csv.cell("+250788000000")).isEqualTo("'+250788000000");
        assertThat(Csv.cell(-5)).isEqualTo("-5");
        assertThat(Csv.cell(null)).isEmpty();
    }

    @Test
    void francAmountsRoundHalfUp() {
        assertThat(Money.lineTotal(new BigDecimal("0.5"), 333)).isEqualTo(167);
        assertThat(Money.lineTotal(new BigDecimal("11.75"), 25_000)).isEqualTo(293_750);
        assertThat(Money.tax(793_750, new BigDecimal("18"))).isEqualTo(142_875);
        assertThat(Money.tax(100, new BigDecimal("7.5"))).isEqualTo(8);
        assertThat(Money.tax(1000, BigDecimal.ZERO)).isZero();
        assertThat(Money.format(1_500_000)).isEqualTo("1,500,000 RWF");
    }
}
