package org.kittykat.cat65.core.cpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CPUWdc65c02Test extends CPUTestBase {
    @Test
    void stzClearsMemory() {
        bus.memory[0x0050] = 0xff;
        loadProgram(0x64, 0x50);  // STZ $50

        assertEquals(3, stepInstruction());
        assertEquals(0x00, bus.memory[0x0050]);
    }
    @Test
    void tsbSetsBitsAndZFlag() {
        bus.memory[0x0060] = 0xf0;
        loadProgram(
                0xa9, 0x0f,  // LDA #$0F
                0x04, 0x60   // TSB $60
        );

        stepInstruction();
        assertEquals(5, stepInstruction());
        assertEquals(0xff, bus.memory[0x0060]);
        assertTrue(flag(FLAG_Z));  // A & original == 0
    }
    @Test
    void trbClearsBitsAndZFlag() {
        bus.memory[0x0060] = 0xff;
        loadProgram(
                0xa9, 0x0f,  // LDA #$0F
                0x14, 0x60   // TRB $60
        );

        stepInstruction();
        stepInstruction();
        assertEquals(0xf0, bus.memory[0x0060]);
        assertFalse(flag(FLAG_Z));  // A & original != 0
    }
    @Test
    void smbSetsSingleMemoryBit() {
        loadProgram(0xb7, 0x70);  // SMB3 $70

        stepInstruction();
        assertEquals(0x08, bus.memory[0x0070]);
    }
    @Test
    void rmbClearsSingleMemoryBit() {
        bus.memory[0x0070] = 0xff;
        loadProgram(0x07, 0x70);  // RMB0 $70

        stepInstruction();
        assertEquals(0xfe, bus.memory[0x0070]);
    }
    @Test
    void bbsBranchesWhenBitIsSet() {
        bus.memory[0x0080] = 0b0000_0100;
        loadProgram(
                0xaf, 0x80, 0x02,  // BBS2 $80, +2
                0xa9, 0x01,        // LDA #$01 (skipped)
                0xa9, 0x02         // LDA #$02
        );

        stepInstructions(2);
        assertEquals(0x02, cpu.getA());
    }
    @Test
    void bbrDoesNotBranchWhenBitIsSet() {
        bus.memory[0x0080] = 0b0000_0100;
        loadProgram(
                0x2f, 0x80, 0x02,  // BBR2 $80, +2
                0xa9, 0x01,        // LDA #$01
                0xa9, 0x02         // LDA #$02
        );

        stepInstructions(2);
        assertEquals(0x01, cpu.getA());
    }
    @Test
    void incAndDecAccumulator() {
        loadProgram(
                0xa9, 0x41,  // LDA #$41
                0x1a,        // INC A
                0x3a         // DEC A
        );

        stepInstructions(2);
        assertEquals(0x42, cpu.getA());
        stepInstruction();
        assertEquals(0x41, cpu.getA());
    }
}
