package org.kittykat.cat65.core.cpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CPUDecimalModeTest extends CPUTestBase {
    @Test
    void adcAddsBCDWithExtraCycle() {
        loadProgram(
                0xf8,        // SED
                0x18,        // CLC
                0xa9, 0x18,  // LDA #$18
                0x69, 0x24   // ADC #$24
        );

        stepInstructions(3);
        assertEquals(3, stepInstruction());  // BCD ADC takes an extra cycle on the 65c02
        assertEquals(0x42, cpu.getA());
        assertFalse(flag(FLAG_C));
    }
    @Test
    void adcBCDWrapsAndSetsCarry() {
        loadProgram(
                0xf8,        // SED
                0x18,        // CLC
                0xa9, 0x99,  // LDA #$99
                0x69, 0x01   // ADC #$01
        );

        stepInstructions(4);
        assertEquals(0x00, cpu.getA());
        assertTrue(flag(FLAG_C));
        assertTrue(flag(FLAG_Z));  // the 65c02 computes valid Z/N flags in decimal mode
    }
    @Test
    void sbcSubtractsBCD() {
        loadProgram(
                0xf8,        // SED
                0x38,        // SEC
                0xa9, 0x42,  // LDA #$42
                0xe9, 0x18   // SBC #$18
        );

        stepInstructions(4);
        assertEquals(0x24, cpu.getA());
        assertTrue(flag(FLAG_C));
    }
    @Test
    void sbcBCDBorrowWrapsAround() {
        loadProgram(
                0xf8,        // SED
                0x38,        // SEC
                0xa9, 0x00,  // LDA #$00
                0xe9, 0x01   // SBC #$01
        );

        stepInstructions(4);
        assertEquals(0x99, cpu.getA());
        assertFalse(flag(FLAG_C));
    }
}
