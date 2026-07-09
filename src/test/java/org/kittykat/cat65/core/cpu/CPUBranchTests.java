package org.kittykat.cat65.core.cpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CPUBranchTests extends CPUTest {
    @Test
    public void testBranchNotTaken() {
        loadProgram(0x18,  // clc
                0xb0, 10  // bcs +10 (not taken)
        );

        stepInstruction();  // clc

        int cycles = stepInstruction();  // bcs
        assertAll(() -> assertEquals(2, cycles, "a branch that isn't taken should take 2 cycles"),
                  () -> assertEquals(PRG_START + 3, cpu.getPC(), "a branch shouldn't offset the PC if it's not taken")
        );
    }
    @Test
    public void testBranchOnSamePage() {
        loadProgram(0x38,  // sec
                0xb0, 15  // bcs +15 (taken, same page)
        );

        stepInstruction();  // sec

        int cycles = stepInstruction();  // bcs
        assertAll(() -> assertEquals(3, cycles, "a branch on the same page should take 3 cycles"),
                  () -> assertEquals(PRG_START + 18, cpu.getPC(), "it should branch to the correct offset")
        );
    }
    @Test
    public void testBranchAcrossPageBoundary() {
        loadProgramAt(0x02f0,
                0x38,  // sec
                0xb0, 18   // bcs +18 (taken, page cross)
        );

        stepInstruction();  // sec

        int cycles = stepInstruction();  // bcs
        assertAll(() -> assertEquals(4, cycles, "a branch across a page boundary should take 4 cycles"),
                  () -> assertEquals(0x0305, cpu.getPC(), "it should branch to the correct offset across the page boundary")
        );
    }
    @Test
    public void testBranchBackwards() {
        loadProgramAt(0x0280,
                0x38,  // sec
                0xb0, -4  // bcs -4 (taken)
        );

        stepInstructions(2);  // sec + bcs
        assertEquals(0x027f, cpu.getPC(), "the branch offset should be signed (twos compliment)");
    }
}
