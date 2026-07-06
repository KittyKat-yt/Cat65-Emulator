package org.kittykat.cat65.core.cpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CPUArithmeticTest extends CPUTestBase {
    @Test
    void adcAddsWithoutCarry() {
        loadProgram(
                0x18,        // CLC
                0xa9, 0x01,  // LDA #$01
                0x69, 0x01   // ADC #$01
        );

        stepInstructions(3);
        assertEquals(0x02, cpu.getA());
        assertFalse(flag(FLAG_C));
    }
    @Test
    void adcAddsCarryIn() {
        loadProgram(
                0x38,        // SEC
                0xa9, 0x01,  // LDA #$01
                0x69, 0x01   // ADC #$01
        );

        stepInstructions(3);
        assertEquals(0x03, cpu.getA());
    }
    @Test
    void adcSetsCarryAndZeroOnWrap() {
        loadProgram(
                0x18,        // CLC
                0xa9, 0xff,  // LDA #$FF
                0x69, 0x01   // ADC #$01
        );

        stepInstructions(3);
        assertEquals(0x00, cpu.getA());
        assertTrue(flag(FLAG_C));
        assertTrue(flag(FLAG_Z));
        assertFalse(flag(FLAG_V));
    }
    @Test
    void adcSetsOverflowOnSignedOverflow() {
        loadProgram(
                0x18,        // CLC
                0xa9, 0x50,  // LDA #$50
                0x69, 0x50   // ADC #$50
        );

        stepInstructions(3);
        assertEquals(0xa0, cpu.getA());
        assertTrue(flag(FLAG_V));
        assertTrue(flag(FLAG_N));
        assertFalse(flag(FLAG_C));
    }
    @Test
    void sbcSubtractsWithBorrowClear() {
        loadProgram(
                0x38,        // SEC
                0xa9, 0x50,  // LDA #$50
                0xe9, 0x10   // SBC #$10
        );

        stepInstructions(3);
        assertEquals(0x40, cpu.getA());
        assertTrue(flag(FLAG_C));
    }
    @Test
    void sbcBorrowClearsCarry() {
        loadProgram(
                0x18,        // CLC (borrow)
                0xa9, 0x50,  // LDA #$50
                0xe9, 0x50   // SBC #$50
        );

        stepInstructions(3);
        assertEquals(0xff, cpu.getA());
        assertFalse(flag(FLAG_C));
        assertTrue(flag(FLAG_N));
    }
    @Test
    void sbcSetsOverflowOnSignedOverflow() {
        loadProgram(
                0x38,        // SEC
                0xa9, 0x50,  // LDA #$50
                0xe9, 0xb0   // SBC #$B0
        );

        stepInstructions(3);
        assertEquals(0xa0, cpu.getA());
        assertTrue(flag(FLAG_V));
        assertFalse(flag(FLAG_C));
    }
    @Test
    void cmpSetsFlagsWithoutChangingA() {
        loadProgram(
                0xa9, 0x40,  // LDA #$40
                0xc9, 0x30,  // CMP #$30
                0xc9, 0x40,  // CMP #$40
                0xc9, 0x50   // CMP #$50
        );

        stepInstructions(2);
        assertTrue(flag(FLAG_C));
        assertFalse(flag(FLAG_Z));

        stepInstruction();
        assertTrue(flag(FLAG_C));
        assertTrue(flag(FLAG_Z));

        stepInstruction();
        assertFalse(flag(FLAG_C));
        assertTrue(flag(FLAG_N));
        assertEquals(0x40, cpu.getA());
    }
    @Test
    void incZeroPageTakes5CyclesAndSetsN() {
        bus.memory[0x0090] = 0x7f;
        loadProgram(0xe6, 0x90);  // INC $90

        assertEquals(5, stepInstruction());
        assertEquals(0x80, bus.memory[0x0090]);
        assertTrue(flag(FLAG_N));
    }
}
