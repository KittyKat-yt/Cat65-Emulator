package org.kittykat.cat65.core.cpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CPUBitwiseTests extends CPUTest {
    @Test
    public void testBitTest() {
        load(0x0040, 0b11000111);
        loadProgram(
                0xa9, 0b00111000,  // lda #%00111000
                0x24, 0x40  // bit $40
        );

        stepInstruction();  // lda

        int cycles = stepInstruction();  // bit
        assertAll(
                () -> assertEquals(3, cycles, "zero-page BIT should take 3 cycles"),
                () -> assertEquals(0b00111000, cpu.getA(), "BIT shouldn't affect the A register"),
                () -> assertEquals(0b11000111, read(0x0040), "BIT is not a RMW instruction"),
                () -> assertTrue(flag('N'), "BIT should copy memory bit 7 into the negative flag"),
                () -> assertTrue(flag('V'), "BIT should copy memory bit 6 into the overflow flag"),
                () -> assertTrue(flag('Z'), "BIT should set the zero flag if (mem & A) == 0")
        );
    }
    @Test
    public void testImmediateBitTest() {
        loadProgram(
                0xa9, 0b00010010,  // lda #%00010010
                0x89, 0b11101101  // bit #%11101101
        );

        stepInstruction();  // lda

        int cycles = stepInstruction();  // bit
        assertAll(
                () -> assertEquals(2, cycles, "immediate BIT should take 2 cycles"),
                () -> assertEquals(0b00010010, cpu.getA(), "BIT shouldn't affect the A register"),
                () -> assertFalse(flag('N'), "BIT immediate should not change the negative flag"),
                () -> assertFalse(flag('V'), "BIT immediate should not change the overflow flag"),
                () -> assertTrue(flag('Z'), "BIT should set the zero flag if (mem & A) == 0")
        );
    }

    @Test
    public void testAND() {
        loadProgram(
            0xa9, 0b11110110,  // lda #%11110110
                0x29, 0b10100111  // and #%10100111
        );

        stepInstruction();  // lda

        int cycles = stepInstruction();  // and
        assertAll(
                () -> assertEquals(2, cycles, "immediate AND should take 2 cycles"),
                () -> assertEquals(0b10100110, cpu.getA(), "AND should correctly bitwise and with the A register")
        );
    }
    @Test
    public void testOR() {
        loadProgram(
            0xa9, 0b01001111,  // lda #%01001111
                0x09, 0b11010101  // ora #%11010101
        );

        stepInstruction();  // lda

        int cycles = stepInstruction();  // ora
        assertAll(
                () -> assertEquals(2, cycles, "immediate ORA should take 2 cycles"),
                () -> assertEquals(0b11011111, cpu.getA(), "ORA should correctly bitwise or with the A register")
        );
    }
    @Test
    public void testExclusiveOR() {
        loadProgram(
            0xa9, 0b10100110,  // lda #%10100110
                0x49, 0b00001111  // eor #%00001111
        );

        stepInstruction();  // lda

        int cycles = stepInstruction();  // eor
        assertAll(
                () -> assertEquals(2, cycles, "immediate EOR should take 2 cycles"),
                () -> assertEquals(0b10101001, cpu.getA(), "EOR should correctly bitwise xor with the A register")
        );
    }

    @Test
    public void testArithmeticShiftLeft() {
        load(0x00c0, 0b01100101);
        loadProgram(
                0x38,  // sec
                0x06, 0xc0  // asl $c0
        );

        stepInstruction();  // sec

        int cycles = stepInstruction();  // asl
        assertAll(
                () -> assertEquals(5, cycles, "zero-page ASL should take 5 cycles"),
                () -> assertEquals(0b11001010, read(0x00c0), "ASL should correctly shift the memory value"),
                () -> assertFalse(flag('C'), "the bit that gets shifted out should be correctly put into the carry flag")
        );
    }
    @Test
    public void testAccumulatorArithmeticShiftLeft() {
        loadProgram(
                0xa9, 0b10101100,  // lda #%10101100
                0x0a  // asl
        );

        stepInstruction();  // lda

        int cycles = stepInstruction();  // asl
        assertAll(
                () -> assertEquals(2, cycles, "accumulator ASL should take 2 cycles"),
                () -> assertEquals(0b01011000, cpu.getA(), "ASL should correctly shift the A register"),
                () -> assertTrue(flag('C'), "the bit that gets shifted out should be correctly put into the carry flag")
        );
    }

    @Test
    public void testLogicalShiftRight() {
        load(0x00c0, 0b11010011);
        loadProgram(
                0x38,  // sec
                0x46, 0xc0  // lsr $c0
        );

        stepInstruction();  // sec

        int cycles = stepInstruction();  // lsr
        assertAll(
                () -> assertEquals(5, cycles, "zero-page LSR should take 5 cycles"),
                () -> assertEquals(0b01101001, read(0x00c0), "LSR should correctly shift the memory value"),
                () -> assertTrue(flag('C'), "the bit that gets shifted out should be correctly put into the carry flag")
        );
    }
    @Test
    public void testAccumulatorLogicalShiftRight() {
        loadProgram(
                0xa9, 0b11101010,  // lda #%11101010
                0x4a  // lsr
        );

        stepInstruction();  // lda

        int cycles = stepInstruction();  // lsr
        assertAll(
                () -> assertEquals(2, cycles, "accumulator LSR should take 2 cycles"),
                () -> assertEquals(0b01110101, cpu.getA(), "LSR should correctly shift the A register"),
                () -> assertFalse(flag('C'), "the bit that gets shifted out should be correctly put into the carry flag")
        );
    }

    @Test
    public void testRotateLeft() {
        load(0x00c0, 0b01011011);
        loadProgram(
                0x38,  // sec
                0x26, 0xc0  // rol $c0
        );

        stepInstruction();  // sec

        int cycles = stepInstruction();  // rol
        assertAll(
                () -> assertEquals(5, cycles, "zero-page ROL should take 5 cycles"),
                () -> assertEquals(0b10110111, read(0x00c0), "ROL should correctly shift the memory value"),
                () -> assertFalse(flag('C'), "the bit that gets shifted out should be correctly put into the carry flag")
        );
    }
    @Test
    public void testAccumulatorRotateLeft() {
        loadProgram(
                0xa9, 0b10101100,  // lda #%10101100
                0x2a  // rol
        );

        stepInstruction();  // lda

        int cycles = stepInstruction();  // rol
        assertAll(
                () -> assertEquals(2, cycles, "accumulator ROL should take 2 cycles"),
                () -> assertEquals(0b01011000, cpu.getA(), "ROL should correctly shift the A register"),
                () -> assertTrue(flag('C'), "the bit that gets shifted out should be correctly put into the carry flag")
        );
    }

    @Test
    public void testRotateRight() {
        load(0x00c0, 0b01100101);
        loadProgram(
                0x38,  // sec
                0x66, 0xc0  // ror $c0
        );

        stepInstruction();  // sec

        int cycles = stepInstruction();  // ror
        assertAll(
                () -> assertEquals(5, cycles, "zero-page ROR should take 5 cycles"),
                () -> assertEquals(0b10110010, read(0x00c0), "ROR should correctly shift the memory value"),
                () -> assertTrue(flag('C'), "the bit that gets shifted out should be correctly put into the carry flag")
        );
    }
    @Test
    public void testAccumulatorRotateRight() {
        loadProgram(
                0xa9, 0b01110110,  // lda #%01110110
                0x6a  // ror
        );

        stepInstruction();  // lda

        int cycles = stepInstruction();  // ror
        assertAll(
                () -> assertEquals(2, cycles, "accumulator ROR should take 2 cycles"),
                () -> assertEquals(0b00111011, cpu.getA(), "ROR should correctly shift the A register"),
                () -> assertFalse(flag('C'), "the bit that gets shifted out should be correctly put into the carry flag")
        );
    }
}
