package org.kittykat.cat65.core.cpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CPUJumpAndStackTest extends CPUTestBase {
    @Test
    void jmpIndirectReadsPointerAcrossPageBoundary() {
        // the 65c02 fixes the NMOS 6502 page-wrap bug: ($02FF) reads $02FF and $0300
        bus.memory[0x02ff] = 0x34;
        bus.memory[0x0300] = 0x12;
        loadProgram(0x6c, 0xff, 0x02);  // JMP ($02FF)

        assertEquals(6, stepInstruction());
        assertEquals(0x1234, cpu.getPC());
    }
    @Test
    void jsrAndRtsRoundTrip() {
        loadProgram(
                0xa2, 0xff,        // LDX #$FF
                0x9a,              // TXS
                0x20, 0x10, 0x02,  // JSR $0210
                0xa9, 0x01         // LDA #$01
        );
        bus.memory[0x0210] = 0xa9;  // LDA #$99
        bus.memory[0x0211] = 0x99;
        bus.memory[0x0212] = 0x60;  // RTS

        stepInstructions(2);
        assertEquals(6, stepInstruction());  // JSR
        assertEquals(0x0210, cpu.getPC());
        assertEquals(0x02, bus.memory[0x01ff]);  // return address high byte
        assertEquals(0x05, bus.memory[0x01fe]);  // return address low byte (PC - 1)

        stepInstruction();
        assertEquals(0x99, cpu.getA());

        assertEquals(6, stepInstruction());  // RTS
        assertEquals(0x0206, cpu.getPC());

        stepInstruction();
        assertEquals(0x01, cpu.getA());
    }
    @Test
    void pushAndPullRoundTrip() {
        loadProgram(
                0xa2, 0xff,  // LDX #$FF
                0x9a,        // TXS
                0xa9, 0x42,  // LDA #$42
                0x48,        // PHA
                0xa9, 0x00,  // LDA #$00
                0x68         // PLA
        );

        stepInstructions(6);
        assertEquals(0x42, cpu.getA());
        assertEquals(0xff, cpu.getS());
        assertFalse(flag(FLAG_Z));
    }
    @Test
    void phxAndPlaTransferThroughStack() {  // 65c02 push/pull variants
        loadProgram(
                0xa2, 0x37,  // LDX #$37
                0xda,        // PHX
                0x68         // PLA
        );

        stepInstructions(3);
        assertEquals(0x37, cpu.getA());
    }
}
