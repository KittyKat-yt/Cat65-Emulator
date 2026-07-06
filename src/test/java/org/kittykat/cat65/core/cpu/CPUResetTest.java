package org.kittykat.cat65.core.cpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CPUResetTest extends CPUTestBase {
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
