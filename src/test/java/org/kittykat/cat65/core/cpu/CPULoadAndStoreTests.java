package org.kittykat.cat65.core.cpu;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CPULoadAndStoreTests extends CPUTest {
    @Test
    public void testLoadImmediate() {
        loadProgram(
                0xa9, 0x5a  // lda #$5a
        );

        int cycles = stepInstruction();  // lda
        assertAll(
                () -> assertEquals(2, cycles, "an immediate LDA should take 2 cycles"),
                () -> assertEquals(0x5a, cpu.getA(), "LDA should load A correctly")
        );
    }
    @Test
    public void testLoadSetsZeroFlag() {
        loadProgram(
                0xa9, 0x00  // lda #$00
        );

        stepInstruction();  // lda
        assertTrue(flag('Z'), "LDA should set the zero flag correctly");
    }
    @Test
    public void testLoadSetsNegativeFlag() {
        loadProgram(
                0xa9, 0x80  // lda #$80
        );

        stepInstruction();  // lda
        assertTrue(flag('N'), "LDA should set the negative flag correctly");
    }

    @Test
    public void testLoadZeroPage() {
        load(0x007f, 0x37);
        loadProgram(
                0xa5, 0x7f  // lda $7f
        );

        int cycles = stepInstruction();  // lda
        assertAll(
                () -> assertEquals(3, cycles, "zero-page LDA should take 3 cycles"),
                () -> assertEquals(0x37, cpu.getA(), "LDA should load A correctly")
        );
    }

    @Test
    public void testIndexedLoad() {
        load(0x8005, 0x10);
        loadProgram(
                0xa2, 0x05,  // ldx #$05
                0xbd, 0x00, 0x80  // lda $8000,x
        );

        stepInstruction();  // ldx

        int cycles = stepInstruction();  // lda
        assertAll(
                () -> assertEquals(4, cycles, "indexed LDA should take 4 cycles"),
                () -> assertEquals(0x10, cpu.getA(), "LDA should load A correctly")
        );
    }
    @Test
    public void testIndexedLoadWithPageCross() {
        load(0x8102, 0x24);
        loadProgram(
                0xa2, 0x12,  // ldx #$12
                0xbd, 0xf0, 0x80  // lda $80f0,x -> $8102
        );

        stepInstruction();  // ldx

        int cycles = stepInstruction();  // lda
        assertAll(
                () -> assertEquals(5, cycles, "indexed LDA across a page boundary should take 5 cycles"),
                () -> assertEquals(0x24, cpu.getA(), "LDA should load A correctly")
        );
    }
    @Test
    public void testIndexedStore() {
        loadProgram(
                0xa2, 0x08,  // ldx #$08
                0xa9, 0x40,  // lda #$40
                0x9d, 0x00, 0x80  // sta $8000,x -> $8008
        );

        stepInstructions(2);  // ldx + lda

        int cycles = stepInstruction();  // sta
        assertAll(
                () -> assertEquals(5, cycles, "indexed STA should always take 5 cycles"),
                () -> assertEquals(0x40, read(0x8008), "STA should store A correctly")
        );
    }

    @Test
    public void testZeroPageIndexedLoad() {
        load(0x0008, 0x5a);
        loadProgram(
                0xa2, 0x10,  // ldx #$10
                0xb5, 0xf8  // lda $f8,x -> $f8 + $10 => $08
        );

        stepInstructions(2);  // ldx + lda
        assertEquals(0x5a, cpu.getA(), "zero-page indexed LDA should load A correctly\n(the address wraps around from $ff to $00)");
    }

    @Test
    public void testIndexedIndirectLoad() {
        load(0x0040,
                0x00, 0x80  // .word $8000
        );
        load(0x8005, 0x95);
        loadProgram(
                0xa0, 0x05,  // ldy #$05
                0xb1, 0x40  // lda ($40),y -> $8005
        );

        stepInstruction();  // ldy

        int cycles = stepInstruction();  // lda
        assertAll(
                () -> assertEquals(5, cycles, "indexed indirect LDA should take 5 cycles"),
                () -> assertEquals(0x95, cpu.getA(), "LDA should load A correctly")
        );
    }
    @Test
    public void testIndirectLoad() {
        load(0x0020,
                0x10, 0x80  // .word $8010
        );
        load(0x8010, 0x7f);
        loadProgram(
                0xb2, 0x20  // lda ($20)
        );

        int cycles = stepInstruction();  // lda
        assertAll(
                () -> assertEquals(5, cycles, "indirect LDA should take 5 cycles"),
                () -> assertEquals(0x7f, cpu.getA(), "LDA should load A correctly")
        );
    }
}
