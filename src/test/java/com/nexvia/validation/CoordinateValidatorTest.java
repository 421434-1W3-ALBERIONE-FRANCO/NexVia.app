package com.nexvia.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoordinateValidatorTest {

    private final LatitudeValidator latValidator = new LatitudeValidator();
    private final LongitudeValidator lngValidator = new LongitudeValidator();

    @Test
    void latitude_null_isValid() {
        assertThat(latValidator.isValid(null, null)).isTrue();
    }

    @Test
    void latitude_validRange_isValid() {
        assertThat(latValidator.isValid(-32.0, null)).isTrue();
        assertThat(latValidator.isValid(0.0, null)).isTrue();
        assertThat(latValidator.isValid(90.0, null)).isTrue();
        assertThat(latValidator.isValid(-90.0, null)).isTrue();
    }

    @Test
    void latitude_outOfRange_isInvalid() {
        assertThat(latValidator.isValid(91.0, null)).isFalse();
        assertThat(latValidator.isValid(-91.0, null)).isFalse();
        assertThat(latValidator.isValid(9999.0, null)).isFalse();
    }

    @Test
    void longitude_null_isValid() {
        assertThat(lngValidator.isValid(null, null)).isTrue();
    }

    @Test
    void longitude_validRange_isValid() {
        assertThat(lngValidator.isValid(-63.0, null)).isTrue();
        assertThat(lngValidator.isValid(0.0, null)).isTrue();
        assertThat(lngValidator.isValid(180.0, null)).isTrue();
        assertThat(lngValidator.isValid(-180.0, null)).isTrue();
    }

    @Test
    void longitude_outOfRange_isInvalid() {
        assertThat(lngValidator.isValid(181.0, null)).isFalse();
        assertThat(lngValidator.isValid(-181.0, null)).isFalse();
        assertThat(lngValidator.isValid(9999.0, null)).isFalse();
    }
}
