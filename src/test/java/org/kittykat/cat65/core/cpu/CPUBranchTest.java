package org.kittykat.cat65.core.cpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CPUBranchTest extends CPUTestBase {
    @Test
    void branchNotTakenTakes2Cycles() {
        loadProgram(
                0xa9, 0x01,  // LDA #$01 (Z clear)
                0xf0, 0x05   // BEQ +5 (not taken)
        );

        stepInstruction();
        assertEquals(2, stepInstruction());
        assertEquals(START + 4, cpu.getPC());
    }
    @Test
    void branchTakenSamePageTakes3Cycles() {
        loadProgram(
                0xa9, 0x00,  // LDA #$00 (Z set)
                0xf0, 0x02   // BEQ +2 (taken, same page)
        );

        stepInstruction();
        assertEquals(3, stepInstruction());
        assertEquals(START + 6, cpu.getPC());
    }
    @Test
    void branchTakenAcrossPageTakes4Cycles() {
        loadProgramAt(0x02fa,
                0xa9, 0x00,  // LDA #$00 (Z set)
                0xf0, 0x10   // BEQ +16 -> $030E (page cross)
        );

        stepInstruction();
        assertEquals(4, stepInstruction());
        assertEquals(0x030e, cpu.getPC());
    }
    @Test
    void braAlwaysBranches() {  // 65c02 instruction
        loadProgram(0x80, 0x02);  // BRA +2

        assertEquals(3, stepInstruction());
        assertEquals(START + 4, cpu.getPC());
    }
}
