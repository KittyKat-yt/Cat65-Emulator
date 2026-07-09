package org.kittykat.cat65.core.cpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CPUArithmeticTests extends CPUTest {
    @Test
    public void testAddWithoutCarry() {
        loadProgram(0x18,  // clc
                0xa9, 0x02,  // lda #$02
                0x69, 0x03  // adc #$03
        );

        stepInstructions(3);  // clc + lda + adc
        assertAll(() -> assertEquals(0x05, cpu.getA(), "ADC should correctly add"),
                  () -> assertFalse(flag('C'), "ADC should clear the carry here")
        );
    }
    @Test
    public void testAddWithCarry() {
        loadProgram(
                0x38,  // sec
                0xa9, 0x01,  // lda #$01
                0x69, 0x01  // adc #$01
        );

        stepInstructions(3);  // sec + lda + adc
        assertAll(() -> assertEquals(0x03, cpu.getA(), "ADC should correctly add with carry"),
                  () -> assertFalse(flag('C'), "ADC should clear the carry here")
        );
    }
    @Test
    public void testAddFlags() {
        loadProgram(
                0x18,  // clc
                0xa9, 0xf0,  // lda #$f0
                0x69, 0x10  // adc #$10
        );

        stepInstructions(3);  // clc + lda + adc
        assertAll(() -> assertEquals(0x00, cpu.getA(), "ADC should correctly add"),
                  () -> assertTrue(flag('C'), "ADC should set the carry flag here"),
                  () -> assertTrue(flag('Z'), "ADC should set the zero flag here"),
                  () -> assertFalse(flag('V'), "ADC should clear the overflow flag here")
        );
    }
    @Test
    public void testAddSignedOverflow() {
        loadProgram(
                0x18,  // clc
                0xa9, 0x50,  // lda #$50
                0x69, 0x50  // adc #$50
        );

        stepInstructions(3);  // clc + lda + adc
        assertAll(() -> assertEquals(0xa0, cpu.getA(), "ADC should correctly add"),
                  () -> assertFalse(flag('C'), "ADC should clear the carry flag here"),
                  () -> assertTrue(flag('V'), "ADC should set the overflow flag here"),
                  () -> assertTrue(flag('N'), "ADC should set the negative flag here")
        );
    }

    @Test
    public void testSubtractWithoutBorrow() {
        loadProgram(
                0x38,  // sec (no borrow)
                0xa9, 0x50,  // lda #$50
                0xe9, 0x10  // sbc #$10
        );

        stepInstructions(3);  // sec + lda + sbc
        assertAll(() -> assertEquals(0x40, cpu.getA(), "SBC should correctly subtract"),
                  () -> assertTrue(flag('C'), "SBC should set the carry here (no borrow)")
        );
    }
    @Test
    public void testSubtractWithBorrow() {
        loadProgram(
                0x18,  // clc (borrow)
                0xa9, 0x50,  // lda #$50
                0xe9, 0x50  // sbc #$50
        );

        stepInstructions(3);  // sec + lda + sbc
        assertAll(() -> assertEquals(0xff, cpu.getA(), "SBC should correctly subtract with borrow"),
                  () -> assertFalse(flag('C'), "SBC should clear the carry here (borrow)")
        );
    }
    @Test
    public void testSubtractSignedOverflow() {
        loadProgram(
                0x38,  // sec (no borrow)
                0xa9, 0x50,  // lda #$50
                0xe9, 0xb0  // sbc #$B0
        );

        stepInstructions(3);  // sec + lda + sbc
        assertAll(() -> assertEquals(0xa0, cpu.getA(), "SBC should correctly subtract"),
                  () -> assertFalse(flag('C'), "SBC should clear the carry flag here (borrow)"),
                  () -> assertTrue(flag('V'), "SBC should set the overflow flag here"),
                  () -> assertTrue(flag('N'), "SBC should set the negative flag here")
        );
    }

    @Test
    public void testCompare() {
        loadProgram(
                0xa9, 0x40,  // lda #$40
                0xc9, 0x30,  // cmp #$30
                0xc9, 0x40,  // cmp #$40
                0xc9, 0x50  // cmp #$50
        );

        stepInstructions(2);  // lda + cmp
        assertAll(() -> assertTrue(flag('C'), "[A > val] CMP should set the carry flag here"),
                  () -> assertFalse(flag('Z'), "[A > val] CMP should clear the zero flag here"),
                  () -> assertFalse(flag('N'), "[A > val] CMP should clear the negative flag here")
        );

        stepInstruction();  // cmp
        assertAll(() -> assertTrue(flag('C'), "[A == val] CMP should set the carry flag here"),
                  () -> assertTrue(flag('Z'), "[A == val] CMP should set the zero flag here"),
                  () -> assertFalse(flag('N'), "[A == val] CMP should clear the negative flag here")
        );

        stepInstruction();  // cmp
        assertAll(() -> assertFalse(flag('C'), "[A < val] CMP should clear the carry flag here"),
                  () -> assertFalse(flag('Z'), "[A < val] CMP should clear the zero flag here"),
                  () -> assertTrue(flag('N'), "[A < val] CMP should set the negative flag here")
        );
        assertEquals(0x40, cpu.getA(), "CMP shouldn't change tha value of the A register");
    }

    @Test
    public void testIncrement() {
        load(0x0090, 0x7f);
        loadProgram(0xe6, 0x90);  // inc $90

        int cycles = stepInstruction();
        assertAll(() -> assertEquals(5, cycles, "zero-page INC should take 5 cycles"),
                  () -> assertEquals(0x80, read(0x0090), "INC should correctly add 1 to the memory value"),
                  () -> assertTrue(flag('N'), "INC should set the negative flag here")
        );
    }
    @Test
    public void testDecrementX() {
        loadProgram(0xa2, 1,  // ldx #1
            0xca  // dex
        );

        stepInstruction();  // ldx

        int cycles = stepInstruction();
        assertAll(() -> assertEquals(2, cycles, "DEX should take 2 cycles"),
                  () -> assertEquals(0x00, read(0x0090), "DEX should correctly subtract 1 from X"),
                  () -> assertTrue(flag('Z'), "INC should set the zero flag here")
        );
    }
}
