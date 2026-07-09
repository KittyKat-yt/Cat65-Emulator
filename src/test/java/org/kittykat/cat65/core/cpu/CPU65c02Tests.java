package org.kittykat.cat65.core.cpu;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CPU65c02Tests extends CPUTest {
    @Test
    public void testStoreZero() {
        load(0x0080, 0xa9);
        loadProgram(0x64, 0x80);  // stz $80

        int cycles = stepInstruction();  // stz
        assertAll(() -> assertEquals(3, cycles, "zero-page STZ should take 3 cycles"),
                  () -> assertEquals(0x00, read(0x0080), "STZ should set the memory value to 0")
        );
    }

    @Test
    public void testTestAndSetBit() {
        load(0x0080, 0xf0);
        loadProgram(0xa9, 0x0f,  // lda #$0f
                0x04, 0x80  // tsb $80
        );

        stepInstruction();  // lda

        int cycles = stepInstruction();  // tsb
        assertAll(() -> assertEquals(5, cycles, "TSB should take 5 cycles"),
                  () -> assertEquals(0xff, read(0x0080), "TSB should correctly write (mem | A) back to that address"),
                  () -> assertTrue(flag('Z'), "TSB should set the zero flag correctly")
        );
    }
    @Test
    public void testTestAndResetBit() {
        load(0x0080, 0xff);
        loadProgram(
                0xa9, 0x0f,  // lda #$0f
                0x14, 0x80  // trb $80
        );

        stepInstruction();  // lda

        int cycles = stepInstruction();  // trb
        assertAll(() -> assertEquals(5, cycles, "TRB should take 5 cycles"),
                () -> assertEquals(0xf0, read(0x0080), "TRB should correctly write (mem & ~A) back to that address"),
                () -> assertFalse(flag('Z'), "TRB should set the zero flag correctly")
        );
    }

    @Test
    public void testSetMemoryBit() {
        loadProgram(0xb7, 0x7f);  // smb3 $70

        int cycles = stepInstruction();  // smb3
        assertAll(() -> assertEquals(5, cycles, "SMB- should take 5 cycles"),
                () -> assertEquals(0x08, read(0x007f), "SMB- should set the right memory bit correctly")
        );
    }
    @Test
    public void testResetMemoryBit() {
        load(0x007f, 0xff);
        loadProgram(0x07, 0x7f);  // rmb0 $70

        int cycles = stepInstruction();  // rmb0
        assertAll(() -> assertEquals(5, cycles, "RMB- should take 5 cycles"),
                () -> assertEquals(0xfe, read(0x007f), "RMB- should reset the right memory bit correctly")
        );
    }

    @Test
    public void testBranchOnBitSet() {
        bus.memory[0x00ff] = 0b0000_0100;
        loadProgram(0xaf, 0xff, 7);  // bbs2 $80, +7 (taken)

        stepInstruction();  // bbs2
        assertEquals(PRG_START + 10, cpu.getPC(), "this BBS- should branch to the correct offset");
    }
    @Test
    public void testBranchOnBitReset() {
        bus.memory[0x0080] = 0b0000_0100;
        loadProgram(0x2f, 0x80, 7);  // bbr2 $80, +7 (not taken)

        stepInstruction();  // bbr2
        assertEquals(PRG_START + 3, cpu.getPC(), "a branch shouldn't offset the PC if it's not taken");
    }

    @Test
    public void testBranchAlways() {
        loadProgram(0x80, 12);  // bra +12

        int cycles = stepInstruction();  // bra
        assertAll(() -> assertEquals(3, cycles, "BRA on the same page should take 3 cycles"),
                  () -> assertEquals(PRG_START + 14, cpu.getPC(), "BRA should go to to the correct offset")
        );
    }

    @Test
    public void testAccumulatorIncrementAndDecrement() {
        loadProgram(0xa9, 0x20,  // lda #$20
                0x1a,  // inc A
                0x3a  // dec A
        );

        stepInstruction();  // lda

        int incCycles = stepInstruction();  // inc
        assertAll(() -> assertEquals(2, incCycles, "INC A should take two cycles"),
                  () -> assertEquals(0x21, cpu.getA(), "INC A should correctly add 1 to A")
        );

        int decCycles = stepInstruction();  // dec
        assertAll(() -> assertEquals(2, decCycles, "DEC A should take two cycles"),
                  () -> assertEquals(0x20, cpu.getA(), "DEC A should correctly subtract 1 from A")
        );
    }
}
