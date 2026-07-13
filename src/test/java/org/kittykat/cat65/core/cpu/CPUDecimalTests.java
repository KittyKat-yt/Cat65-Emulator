package org.kittykat.cat65.core.cpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CPUDecimalTests extends CPUTest {
    @Test
    public void testDecimalAdd() {
        loadProgram(
                0xf8,  // sed
                0x18,  // clc
                0xa9, 0x16,  // lda #$16
                0x69, 0x24  // adc #$24
        );

        stepInstructions(3);  // sed + clc + lda

        int cycles = stepInstruction();  // adc
        assertAll(
                () -> assertEquals(3, cycles, "decimal ADC should take 3 cycles"),
                () -> assertEquals(0x40, cpu.getA(), "decimal ADC should add in BCD correctly"),
                () -> assertFalse(flag('C'), "the carry flag shouldn't be set if there was no wraparound")
        );
    }
    @Test
    public void testDecimalAddWraparound() {
        loadProgram(
                0xf8, // sed
                0x18,  // clc
                0xa9, 0x95,  // lda #$95
                0x69, 0x08  // adc #$08
        );

        stepInstructions(4);  // sed + clc + lda + adc
        assertAll(
                () -> assertEquals(0x03, cpu.getA(), "decimal ADC should wrap around correctly in BCD"),
                () -> assertTrue(flag('C'), "decimal ADC wraparound should set the carry flag")
        );
    }

    @Test
    public void testDecimalSubtract() {
        loadProgram(
                0xf8,  // sed
                0x38,  // sec
                0xa9, 0x45,  // lda #$45
                0xe9, 0x15  // sbc #$15
        );

        stepInstructions(3);  // sed + sec + lda

        int cycles = stepInstruction();  // sbc
        assertAll(
                () -> assertEquals(3, cycles, "decimal SBC should take 3 cycles"),
                () -> assertEquals(0x30, cpu.getA(), "decimal SBC should subtract in BCD correctly"),
                () -> assertTrue(flag('C'), "the carry flag shouldn't be cleared if there was no wraparound")
        );
    }
    @Test
    public void testDecimalSubtractWraparound() {
        loadProgram(
                0xf8,  // sed
                0x38,  // sec
                0xa9, 0x01,  // lda #$01
                0xe9, 0x03  // sbc #$03
        );

        stepInstructions(4);  // sed + sec + lda + sbc
        assertAll(
                () -> assertEquals(0x98, cpu.getA(), "decimal SBC should wrap around correctly in BCD"),
                () -> assertFalse(flag('C'), "decimal SBC wraparound should clear the carry flag")
        );
    }
}
