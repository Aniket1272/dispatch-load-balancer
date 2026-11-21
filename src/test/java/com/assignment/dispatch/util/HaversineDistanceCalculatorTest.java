package com.assignment.dispatch.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class HaversineDistanceCalculatorTest {

    private final HaversineDistanceCalculator calculator = new HaversineDistanceCalculator();

    @Test
    void samePointHasZeroDistance() {
        assertThat(calculator.distanceKm(28.6139, 77.2090, 28.6139, 77.2090)).isZero();
    }

    @Test
    void connaughtPlaceToKarolBagh() {
        assertThat(calculator.distanceKm(28.6139, 77.2090, 28.7041, 77.1025))
                .isCloseTo(14.44, within(0.05));
    }

    @Test
    void bangaloreToChennai() {
        assertThat(calculator.distanceKm(12.9716, 77.5946, 13.0827, 80.2707))
                .isCloseTo(290.17, within(0.1));
    }

    @Test
    void delhiToMumbai() {
        assertThat(calculator.distanceKm(28.6139, 77.2090, 19.0760, 72.8777))
                .isCloseTo(1148.09, within(0.5));
    }

    @Test
    void distanceIsSymmetric() {
        double ab = calculator.distanceKm(12.9716, 77.5946, 28.6139, 77.2090);
        double ba = calculator.distanceKm(28.6139, 77.2090, 12.9716, 77.5946);
        assertThat(ab).isEqualTo(ba, within(1e-9));
    }

    @Test
    void antipodalPointsAreHalfTheEarthCircumference() {
        assertThat(calculator.distanceKm(0, 0, 0, 180)).isCloseTo(20015.09, within(0.5));
    }

    @Test
    void rejectsOutOfRangeLatitude() {
        assertThatThrownBy(() -> calculator.distanceKm(91, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsOutOfRangeLongitude() {
        assertThatThrownBy(() -> calculator.distanceKm(0, 0, 0, 181))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNaN() {
        assertThatThrownBy(() -> calculator.distanceKm(Double.NaN, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
