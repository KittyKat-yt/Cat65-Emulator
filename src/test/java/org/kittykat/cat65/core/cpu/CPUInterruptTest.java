package org.kittykat.cat65.core.cpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CPUInterruptTest extends CPUTestBase {
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
