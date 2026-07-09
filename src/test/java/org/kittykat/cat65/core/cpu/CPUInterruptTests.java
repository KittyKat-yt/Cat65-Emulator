package org.kittykat.cat65.core.cpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CPUInterruptTests extends CPUTest {
    @Test
    public void testBRK() {
        loadVector(VEC_IRQ, 0x0300);
        loadProgram(0x58,  // cli
                    0xf8,  // sed
                    0x00, 0xea  // brk + padding byte
        );

        stepInstructions(2);  // cli + sed

        int cycles = stepInstruction();  // brk
        assertAll(() -> assertEquals(7, cycles, "BRK should take 7 cycles"),
                  () -> assertEquals(0x0300, cpu.getPC(), "BRK should correctly load the IRQ vector"),
                  () -> assertTrue(flag('I'), "the interrupt disable flag should be set"),
                  () -> assertFalse(flag('D'), "the decimal flag should be cleared")
        );

        int pushedP = readStack(0);
        assertAll(() -> assertEquals(PRG_START + 4, readStackWord(1), "BRK should push the correct return address"),
                  () -> assertEquals(0x10, pushedP & 0x10, "the B-flag should be pushed as 1 by BRK"),
                  () -> assertEquals(0x20, pushedP & 0x20, "bit 5 of P should've been pushed as 1"),
                  () -> assertEquals(0x38, pushedP, "the correct status value should've been pushed to the stack")
        );
    }
    @Test
    public void testInterruptDisableBRK() {
        loadVector(VEC_IRQ, 0x0300);
        loadProgram(0x00, 0xea);  // brk + padding byte

        // 'I' should be set from Reset
        stepInstruction();  // brk
        assertEquals(0x0300, cpu.getPC(), "BRK should still run when interrupts are disabled");
    }

    @Test
    public void testIRQ() {
        loadVector(VEC_IRQ, 0x0300);
        loadProgram(0x58,  // cli
                0xf8,  // sed
                0xea  // nop
        );

        stepInstructions(2);  // cli + sed

        bus.irq = true;
        int cycles = stepInstruction();  // IRQ
        assertAll(() -> assertEquals(7, cycles, "IRQs should take 7 cycles"),
                  () -> assertEquals(0x0300, cpu.getPC(), "IRQs should correctly load the IRQ vector"),
                  () -> assertTrue(flag('I'), "the interrupt disable flag should be set"),
                  () -> assertFalse(flag('D'), "the decimal flag should be cleared")
        );

        int pushedP = readStack(0);
        assertAll(() -> assertEquals(PRG_START + 2, readStackWord(1), "IRQs should push the correct return address"),
                  () -> assertEquals(0x00, pushedP & 0x10, "the B-flag should be pushed as 0 by IRQs"),
                  () -> assertEquals(0x20, pushedP & 0x20, "bit 5 of P should've been pushed as 1"),
                  () -> assertEquals(0x28, pushedP, "the correct status value should've been pushed to the stack")
        );
    }
    @Test
    public void testMaskedIRQ() {
        loadVector(VEC_IRQ, 0x0300);
        loadProgram(0xea);  // nop

        // 'I' should be set from Reset
        bus.irq = true;
        stepInstruction();  // IRQ
        assertEquals(PRG_START + 1, cpu.getPC(), "IRQs should get masked by the interrupt disable flag");
    }

    @Test
    public void testNMI() {
        loadVector(VEC_NMI, 0x0300);
        loadProgram(0x58,  // cli
                0xf8,  // sed
                0xea  // nop
        );

        stepInstructions(2);  // cli + sed

        bus.nmi = true;
        int cycles = stepInstruction();  // NMI
        assertAll(() -> assertEquals(7, cycles, "NMIs should take 7 cycles"),
                  () -> assertEquals(0x0300, cpu.getPC(), "NMIs should correctly load the NMI vector"),
                  () -> assertTrue(flag('I'), "the interrupt disable flag should be set"),
                  () -> assertFalse(flag('D'), "the decimal flag should be cleared")
        );

        int pushedP = readStack(0);
        assertAll(() -> assertEquals(PRG_START + 2, readStackWord(1), "NMIs should push the correct return address"),
                  () -> assertEquals(0x00, pushedP & 0x10, "the B-flag should be pushed as 0 by NMIs"),
                  () -> assertEquals(0x20, pushedP & 0x20, "bit 5 of P should've been pushed as 1"),
                  () -> assertEquals(0x28, pushedP, "the correct status value should've been pushed to the stack")
        );
    }
    @Test
    public void testUnmaskableNMI() {
        loadVector(VEC_NMI, 0x0300);
        loadProgram(0xea);  // nop

        // 'I' should be set from Reset
        bus.nmi = true;
        stepInstruction();  // NMI
        assertEquals(0x0300, cpu.getPC(), "NMIs shouldn't get masked by the interrupt disable flag");
    }

    @Test
    public void testInterruptPriority() {
        loadVector(VEC_IRQ, 0x0300);
        loadVector(VEC_NMI, 0x0400);
        loadProgram(0x58,  // cli
                0xea  // nop
        );

        stepInstruction();  // cli
        bus.irq = true;
        bus.nmi = true;

        stepInstruction();  // NMI
        assertEquals(0x0400, cpu.getPC(), "NMIs should have priority over IRQs");
    }

    @Test
    public void testReturnFromInterrupt() {
        loadProgram(0x40);  // rti
        loadStack(0xff,  // status
                0x80, 0x03  // return address
        );

        int cycles = stepInstruction();  // rti
        assertAll(() -> assertEquals(6, cycles, "RTI should take 6 cycles"),
                  () -> assertEquals(0x0380, cpu.getPC(), "RTI should return the PC to the correct address"),
                  // bit 5 and 'B' are ignored by this test
                  () -> assertEquals(0xcf, cpu.getP() & 0xcf, "RTI should correctly set the status flags")
        );
    }

    @Test
    public void testWait() {
        loadProgram(0xcb);  // wai

        int cycles = stepInstruction();  // wai
        assertEquals(3, cycles, "WAI should take 3 cycles");

        int pc = cpu.getPC();
        stepInstruction();
        assertEquals(pc, cpu.getPC(), "the CPU should not execute anything while waiting");
    }
    @Test
    public void testWaitIRQ() {
        loadVector(VEC_IRQ, 0x0300);
        loadProgram(0x58,  // cli
                0xcb  // wai
        );

        stepInstructions(3);  // cli + wai + extra

        bus.irq = true;
        stepInstruction();  // IRQ
        assertEquals(0x0300, cpu.getPC(), "IRQs should \"wake up\" the CPU from waiting");
    }
    @Test
    public void testWaitDisabledIRQ() {
        loadVector(VEC_IRQ, 0x0300);
        loadProgram(0xcb,  // wai
                0xea  // nop
        );

        stepInstructions(2);  // wai + extra

        // 'I' should be set from Reset
        bus.irq = true;
        stepInstruction();  // nop
        assertEquals(PRG_START + 2, cpu.getPC(), "IRQs should \"wake up\" the CPU from waiting, even when IRQs are disabled");
    }
    @Test
    public void testWaitNMI() {
        loadVector(VEC_NMI, 0x0300);
        loadProgram(0xcb);  // wai

        stepInstructions(2);  // wai + extra

        bus.nmi = true;
        stepInstruction();  // NMI
        assertEquals(0x0300, cpu.getPC(), "NMIs should \"wake up\" the CPU from waiting");
    }

    @Test
    public void testStop() {
        loadProgram(0xea,  // nop
                0xdb  // stp
        );

        stepInstruction();  // nop

        int cycles = stepInstruction();  // stp
        assertEquals(3, cycles, "SPT should take 3 cycles");

        int pc = cpu.getPC();
        stepInstruction();
        assertEquals(pc, cpu.getPC(), "the CPU should not execute anything while stopped");

        bus.irq = true;
        bus.nmi = true;
        stepInstruction();
        assertEquals(pc, cpu.getPC(), "interrupts should not be serviced while stopped");

        reset();
        assertEquals(PRG_START, cpu.getPC(), "Reset should \"wake up\" the CPU from being stopped");
    }
}
