package io.github.michdo93.openhab.testsuite;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Validators of ItemTester (no openHAB needed). */
class ValidatorTest {

    @Test
    void switchContactPlayer() {
        assertTrue(ItemTester.isValidSwitchValue("on"));
        assertFalse(ItemTester.isValidSwitchValue("AN"));
        assertTrue(ItemTester.isValidContactValue("CLOSED"));
        assertTrue(ItemTester.isValidPlayerValue("PLAY"));
        assertFalse(ItemTester.isValidPlayerValue(null));
    }

    @Test
    void numbersAndPercent() {
        assertTrue(ItemTester.isValidDimmerValue(0));
        assertTrue(ItemTester.isValidDimmerValue("55"));
        assertFalse(ItemTester.isValidDimmerValue("101"));
        assertTrue(ItemTester.isValidRollershutterValue("DOWN"));
        assertTrue(ItemTester.isValidNumberValue("21.5 °C"));
        assertTrue(ItemTester.isValidNumberValue(-3));
        assertFalse(ItemTester.isValidNumberValue("warm"));
    }

    @Test
    void colorLocationDateImage() {
        assertTrue(ItemTester.isValidColorValue("240,100,100"));
        assertFalse(ItemTester.isValidColorValue("400,0,0"));
        assertTrue(ItemTester.isValidLocationValue("48.7758,9.1829"));
        assertFalse(ItemTester.isValidLocationValue("91,0"));
        assertTrue(ItemTester.isValidDateTimeValue("2024-01-15T08:30:00+0000"));
        assertTrue(ItemTester.isValidDateTimeValue("2024-01-15T08:30:00.000Z"));
        assertFalse(ItemTester.isValidDateTimeValue("15.01.2024"));
        assertTrue(ItemTester.isValidImageValue("data:image/png;base64,iVBOR"));
        assertFalse(ItemTester.isValidImageValue("bild.png"));
    }
}
