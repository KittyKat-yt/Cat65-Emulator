package org.kittykat.cat65.core.cpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CPULoadStoreTest extends CPUTestBase {
    @Test
    void ldaImmediateSetsAccumulatorAndFlags() {
        loadProgram(0xa9, 0x42);  // LDA #$42

        assertEquals(2, stepInstruction());
        assertEquals(0x42, cpu.getA());
        assertFalse(flag(FLAG_Z));
        assertFalse(flag(FLAG_N));
    }
    @Test
    void ldaImmediateZeroSetsZ() {
        loadProgram(0xa9, 0x00);  // LDA #$00

        stepInstruction();
        assertTrue(flag(FLAG_Z));
    }
    @Test
    void ldaImmediateNegativeSetsN() {
        loadProgram(0xa9, 0x80);  // LDA #$80

        stepInstruction();
        assertTrue(flag(FLAG_N));
    }
    @Test
    void ldaZeroPageTakes3Cycles() {
        bus.memory[0x0042] = 0x37;
        loadProgram(0xa5, 0x42);  // LDA $42

        assertEquals(3, stepInstruction());
        assertEquals(0x37, cpu.getA());
    }
    @Test
    void ldaAbsoluteXWithoutPageCrossTakes4Cycles() {
        bus.memory[0x8001] = 0x11;
        loadProgram(
                0xa2, 0x01,        // LDX #$01
                0xbd, 0x00, 0x80   // LDA $8000,X
        );

        stepInstruction();
        assertEquals(4, stepInstruction());
        assertEquals(0x11, cpu.getA());
    }
    @Test
    void ldaAbsoluteXPageCrossTakesExtraCycle() {
        bus.memory[0x8100] = 0x22;
        loadProgram(
                0xa2, 0x01,        // LDX #$01
                0xbd, 0xff, 0x80   // LDA $80FF,X -> $8100
        );

        stepInstruction();
        assertEquals(5, stepInstruction());
        assertEquals(0x22, cpu.getA());
    }
    @Test
    void staAbsoluteXAlwaysTakes5Cycles() {
        loadProgram(
                0xa2, 0x01,        // LDX #$01
                0xa9, 0x42,        // LDA #$42
                0x9d, 0xff, 0x80   // STA $80FF,X -> $8100
        );

        stepInstructions(2);
        assertEquals(5, stepInstruction());
        assertEquals(0x42, bus.memory[0x8100]);
    }
    @Test
    void zeroPageIndexedWrapsAround() {
        bus.memory[0x0008] = 0x5a;
        loadProgram(
                0xa2, 0x10,  // LDX #$10
                0xb5, 0xf8   // LDA $F8,X -> ($F8 + $10) & $FF = $08
        );

        stepInstructions(2);
        assertEquals(0x5a, cpu.getA());
    }
    @Test
    void indirectIndexedReadsThroughPointer() {
        bus.memory[0x0040] = 0x00;
        bus.memory[0x0041] = 0x80;
        bus.memory[0x8005] = 0x99;
        loadProgram(
                0xa0, 0x05,  // LDY #$05
                0xb1, 0x40   // LDA ($40),Y -> $8005
        );

        stepInstruction();
        assertEquals(5, stepInstruction());
        assertEquals(0x99, cpu.getA());
    }
    @Test
    void zeroPageIndirectReadsThroughPointer() {  // 65c02 addressing mode
        bus.memory[0x0040] = 0x05;
        bus.memory[0x0041] = 0x80;
        bus.memory[0x8005] = 0x77;
        loadProgram(0xb2, 0x40);  // LDA ($40)

        assertEquals(5, stepInstruction());
        assertEquals(0x77, cpu.getA());
    }
}
