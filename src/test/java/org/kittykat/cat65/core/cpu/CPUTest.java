package org.kittykat.cat65.core.cpu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless 65c02 core tests running against {@link TestBus} — no JavaFX needed.
**/
class CPUTest {
    private static final int START = 0x0200;

    private static final int FLAG_N = 0x80;
    private static final int FLAG_V = 0x40;
    private static final int FLAG_D = 0x08;
    private static final int FLAG_I = 0x04;
    private static final int FLAG_Z = 0x02;
    private static final int FLAG_C = 0x01;

    private TestBus bus;
    private CPU cpu;

    @BeforeEach
    void makeCPU() {
        bus = new TestBus();
        cpu = new CPU(bus);
    }

    private void loadProgram(int... bytes) {
        loadProgramAt(START, bytes);
    }
    private void loadProgramAt(int address, int... bytes) {
        for (int i = 0; i < bytes.length; i++) {
            bus.memory[(address + i) & 0xffff] = bytes[i] & 0xff;
        }
        bus.memory[0xfffc] = address & 0xff;
        bus.memory[0xfffd] = (address >> 8) & 0xff;
        reset();
    }
    private int reset() {
        cpu.reset();
        return runToBoundary();
    }
    /**
     * Executes one full instruction (or interrupt sequence) and returns how many clock ticks it took.
    **/
    private int stepInstruction() {
        cpu.clock();
        return runToBoundary() + 1;
    }
    private void stepInstructions(int count) {
        for (int i = 0; i < count; i++) {
            stepInstruction();
        }
    }
    private int runToBoundary() {
        int ticks = 0;
        while (!cpu.isAtInstructionBoundary()) {
            cpu.clock();
            ticks++;
        }
        return ticks;
    }
    private boolean flag(int mask) {
        return (cpu.getP() & mask) != 0;
    }

    @Nested
    class Reset {
        @Test
        void loadsPCFromResetVectorAndTakes7Cycles() {
            bus.memory[0xfffc] = 0x00;
            bus.memory[0xfffd] = 0x80;

            cpu.reset();
            int ticks = runToBoundary();

            assertEquals(7, ticks);
            assertEquals(0x8000, cpu.getPC());
        }
        @Test
        void setsInterruptDisableAndClearsDecimal() {
            loadProgram(0xea);  // NOP

            assertTrue(flag(FLAG_I));
            assertFalse(flag(FLAG_D));
        }
        @Test
        void decrementsStackPointerByThree() {
            loadProgram(0xea);  // NOP

            assertEquals(0xfd, cpu.getS());  // S starts at 0x00, reset performs 3 suppressed pushes
        }
    }

    @Nested
    class LoadStore {
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

    @Nested
    class Arithmetic {
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

    @Nested
    class DecimalMode {
        @Test
        void adcAddsBCDWithExtraCycle() {
            loadProgram(
                    0xf8,        // SED
                    0x18,        // CLC
                    0xa9, 0x18,  // LDA #$18
                    0x69, 0x24   // ADC #$24
            );

            stepInstructions(3);
            assertEquals(3, stepInstruction());  // BCD ADC takes an extra cycle on the 65c02
            assertEquals(0x42, cpu.getA());
            assertFalse(flag(FLAG_C));
        }
        @Test
        void adcBCDWrapsAndSetsCarry() {
            loadProgram(
                    0xf8,        // SED
                    0x18,        // CLC
                    0xa9, 0x99,  // LDA #$99
                    0x69, 0x01   // ADC #$01
            );

            stepInstructions(4);
            assertEquals(0x00, cpu.getA());
            assertTrue(flag(FLAG_C));
            assertTrue(flag(FLAG_Z));  // the 65c02 computes valid Z/N flags in decimal mode
        }
        @Test
        void sbcSubtractsBCD() {
            loadProgram(
                    0xf8,        // SED
                    0x38,        // SEC
                    0xa9, 0x42,  // LDA #$42
                    0xe9, 0x18   // SBC #$18
            );

            stepInstructions(4);
            assertEquals(0x24, cpu.getA());
            assertTrue(flag(FLAG_C));
        }
        @Test
        void sbcBCDBorrowWrapsAround() {
            loadProgram(
                    0xf8,        // SED
                    0x38,        // SEC
                    0xa9, 0x00,  // LDA #$00
                    0xe9, 0x01   // SBC #$01
            );

            stepInstructions(4);
            assertEquals(0x99, cpu.getA());
            assertFalse(flag(FLAG_C));
        }
    }

    @Nested
    class Branches {
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

    @Nested
    class JumpsAndStack {
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

    @Nested
    class Interrupts {
        @Test
        void brkPushesStateAndVectorsThroughFFFE() {
            bus.memory[0xfffe] = 0x00;
            bus.memory[0xffff] = 0x03;
            bus.memory[0x0300] = 0x40;  // RTI
            loadProgram(
                    0x00, 0xea,  // BRK + padding byte
                    0xa9, 0x42   // LDA #$42
            );

            assertEquals(7, stepInstruction());  // BRK
            assertEquals(0x0300, cpu.getPC());
            assertTrue(flag(FLAG_I));
            // BRK pushes P with bit 5 and the B flag set (S was 0xFD after reset)
            assertEquals(0x34, bus.memory[0x01fb]);

            assertEquals(6, stepInstruction());  // RTI
            assertEquals(START + 2, cpu.getPC());

            stepInstruction();
            assertEquals(0x42, cpu.getA());
        }
        @Test
        void irqIsServicedWhenInterruptsAreEnabled() {
            bus.memory[0xfffe] = 0x00;
            bus.memory[0xffff] = 0x03;
            bus.memory[0x0300] = 0x40;  // RTI
            loadProgram(
                    0x58,  // CLI
                    0xea   // NOP
            );

            stepInstruction();  // CLI
            bus.irq = true;

            assertEquals(7, stepInstruction());  // IRQ sequence
            assertEquals(0x0300, cpu.getPC());
            assertTrue(flag(FLAG_I));
            int pushedP = bus.memory[0x01fb];
            assertEquals(0x00, pushedP & 0x10);  // hardware interrupts push the B flag as 0
            assertEquals(0x20, pushedP & 0x20);  // ...but bit 5 as 1

            bus.irq = false;
            stepInstruction();  // RTI
            assertEquals(START + 1, cpu.getPC());
            assertFalse(flag(FLAG_I));
        }
        @Test
        void irqIsMaskedByInterruptDisable() {
            loadProgram(0xea, 0xea);  // NOP, NOP

            bus.irq = true;  // I is still set from reset
            stepInstruction();
            assertEquals(START + 1, cpu.getPC());  // NOP executed, no interrupt taken
        }
        @Test
        void nmiIgnoresInterruptDisable() {
            bus.memory[0xfffa] = 0x10;
            bus.memory[0xfffb] = 0x03;
            loadProgram(0xea, 0xea);  // NOP, NOP

            bus.nmi = true;  // I is still set from reset
            assertEquals(7, stepInstruction());
            assertEquals(0x0310, cpu.getPC());
        }
        @Test
        void nmiHasPriorityOverIrq() {
            bus.memory[0xfffa] = 0x10;
            bus.memory[0xfffb] = 0x03;
            bus.memory[0xfffe] = 0x00;
            bus.memory[0xffff] = 0x03;
            loadProgram(
                    0x58,  // CLI
                    0xea   // NOP
            );

            stepInstruction();  // CLI
            bus.irq = true;
            bus.nmi = true;

            stepInstruction();
            assertEquals(0x0310, cpu.getPC());  // NMI vector, not IRQ vector
        }
        @Test
        void waiHaltsUntilInterrupt() {
            loadProgram(
                    0xcb,       // WAI
                    0xa9, 0x01  // LDA #$01
            );

            assertEquals(3, stepInstruction());  // WAI
            assertTrue(cpu.isWaiting());
            int pc = cpu.getPC();

            stepInstruction();
            assertEquals(pc, cpu.getPC());  // still waiting, nothing executes

            // an IRQ resumes execution; since I is set it is not serviced (SEI+WAI idiom)
            bus.irq = true;
            stepInstruction();
            assertFalse(cpu.isWaiting());
            assertEquals(0x01, cpu.getA());
        }
        @Test
        void stpStopsTheProcessorForGood() {
            loadProgram(
                    0xdb,       // STP
                    0xa9, 0x01  // LDA #$01
            );

            assertEquals(3, stepInstruction());  // STP
            assertTrue(cpu.isStopped());
            int pc = cpu.getPC();

            bus.irq = true;
            stepInstructions(3);
            assertEquals(pc, cpu.getPC());  // not even interrupts wake it up
            assertEquals(0x00, cpu.getA());
        }
    }

    @Nested
    class Wdc65c02Instructions {
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
}
