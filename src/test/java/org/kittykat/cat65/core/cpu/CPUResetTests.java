package org.kittykat.cat65.core.cpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CPUResetTests extends CPUTest {
    @Test
    public void testBaseReset() {
        loadVector(VEC_RESET, 0x1234);
        cpu.reset();

        assertAll(
                () -> assertEquals(7, runToBoundary(), "Reset should take 7 cycles"),
                () -> assertEquals(0x1234, cpu.getPC(), "Reset should correctly load the RESET vector")
        );
    }

    @Test
    public void testFlagsReset() {
        loadProgram();

        assertAll(
                () -> assertTrue(flag('I'), "the interrupt disable flag should be set"),
                () -> assertFalse(flag('D'), "the decimal flag should be cleared")
        );
    }

    @Test
    public void testResetStackDecrement() {
        loadProgram();

        // assumes that S started at 0x00
        assertEquals(0xfd, cpu.getS(), "the stack pointer should be decremented 3 times");
    }
}
