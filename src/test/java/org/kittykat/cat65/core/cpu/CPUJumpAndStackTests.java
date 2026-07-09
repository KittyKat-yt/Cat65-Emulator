package org.kittykat.cat65.core.cpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CPUJumpAndStackTests extends CPUTest {
    @Test
    public void testJump() {
        loadProgram(0x4c, 0x34, 0x12);  // jmp $1234

        int cycles = stepInstruction();  // jmp
        assertAll(() -> assertEquals(3, cycles, "an absolute jump should take 3 cycles"),
                  () -> assertEquals(0x1234, cpu.getPC(), "JMP should move the PC to the correct target address")
        );
    }
    @Test
    public void testIndirectJump() {
        loadProgram(0x6c, 0x80, 0x02);  // jmp ($0280)
        load(0x0280, 0x34, 0x12);  // .word $1234

        int cycles = stepInstruction();  // jmp
        assertAll(() -> assertEquals(6, cycles, "an indirect jump should always take 6 cycles"),
                  () -> assertEquals(0x1234, cpu.getPC(), "JMP should move the PC to the correct target address")
        );
    }
    @Test
    public void testPageBoundaryIndirectJump() {
        loadProgram(0x6c, 0xff, 0x02);  // jmp ($02ff)
        load(0x02ff, 0x34, 0x12);  // .word $1234

        int cycles = stepInstruction();  // jmp
        assertAll(() -> assertEquals(6, cycles, "an indirect jump should always take 6 cycles"),
                  () -> assertEquals(0x1234, cpu.getPC(), "JMP should move the PC to the correct target address\n" +
                          "also, on the 65c02 the indirect address lookup does fix the high byte of the address during the lookup")
        );
    }
    @Test
    public void testJumpTable() {
        loadProgram(0xa2, 2,  // ldx #2
                0x7c, 0x00, 0x03  // jmp ($0300,x)
        );
        load(0x0300,
                0x0f, 0xf0,  // .word $f00f
                0x34, 0x12,  // .word $1234
                0x00, 0x80  // .word $8000
        );

        stepInstruction();  // ldx

        int cycles = stepInstruction();
        assertAll(() -> assertEquals(6, cycles, "in indirect indexed jump should take 6 cycles"),
                  () -> assertEquals(0x1234, cpu.getPC(),
                          "an indirect indexed jump should read and move the PC to the correct target address")
        );
    }

    @Test
    public void testSubRoutine() {
        loadProgram(0x20, 0x00, 0x03);  // jsr $0300
        load(0x0300, 0x60);  // rts

        int jsrCycles = stepInstruction();  // jsr
        assertAll(() -> assertEquals(6, jsrCycles, "JSR should take 6 cycles"),
                  () -> assertEquals(0x0300, cpu.getPC(), "JSR should move the PC to the correct target address"),
                  () -> assertEquals(PRG_START + 2, readStackWord(0), "JSR should push the correct return address")
        );

        int rtsCycles = stepInstruction();  // rts
        assertAll(() -> assertEquals(6, rtsCycles, "RTS should take 6 cycles"),
                  () -> assertEquals(PRG_START + 3, cpu.getPC(), "RTS should return the PC to the correct address")
        );
    }

    @Test
    public void testPushAndPullStatus() {
        loadProgram(0x38,  // sec
                0x78,  // sei
                0xf8,  // sed
                0x08,  // php
                0x28  // plp
        );

        stepInstructions(4);  // sec + sei + sed + php
        int pushedP = readStack(0);
        assertAll(() -> assertEquals(0x10, pushedP & 0x10, "the B-flag should be pushed as 1 by PHPs"),
                  () -> assertEquals(0x20, pushedP & 0x20, "bit 5 of P should've been pushed as 1"),
                  () -> assertEquals(0x3d, pushedP, "the correct status value should've been pushed to the stack")
        );

        loadStack(0xff);  // status
        stepInstruction();  // plp
        // bit 5 and 'B' are ignored by this test
        assertEquals(0xcf, cpu.getP() & 0xcf, "RTI should correctly set the status flags");
    }

    @Test
    public void testStackPointerTransfers() {
        loadProgram(0xba,  // tsx
                0xa2, 0xef,  // ldx #$ef
                0x9a  // txs
        );

        int tsxCycles = stepInstruction();  // tsx
        assertAll(() -> assertEquals(2, tsxCycles, "TSX should take 2 cycles"),
                  () -> assertEquals(cpu.getS(), cpu.getX(), "TSX should transfer the correct stack pointer value to X")
        );

        stepInstruction();  // ldx

        int txsCycles = stepInstruction();
        assertAll(() -> assertEquals(2, txsCycles, "TXS should take 2 cycles"),
                  () -> assertEquals(cpu.getX(), cpu.getS(), "TXS should transfer the value from X to the stack pointer")
        );
    }

    @Test
    public void testStackRegisters() {
        loadProgram(0xa9, 0xf5,  // lda #$f5
                0xa2, 0x24,  // ldx #$24
                0xa0, 0xca,  // ldy #$ca
                0x48,  // pha
                0xda,  // phx
                0x5a,  // phy
                0x68,  // pla
                0x7a,  // ply
                0xfa  // plx
        );

        stepInstructions(6);  // 3 loads + 3 pushes
        assertAll(() -> assertEquals(0xf5, readStack(2), "the correct A value should've been pushed"),
                  () -> assertEquals(0x24, readStack(1), "the correct X value should've been pushed"),
                  () -> assertEquals(0xca, readStack(0), "the correct Y value should've been pushed")
        );

        stepInstructions(3);  // 3 pulls
        assertAll(() -> assertEquals(0xca, cpu.getA(), "the correct value should've been pulled into A"),
                  () -> assertEquals(0xf5, cpu.getX(), "the correct value should've been pulled into X"),
                  () -> assertEquals(0x24, cpu.getY(), "the correct value should've been pulled into Y")
        );
    }
}
