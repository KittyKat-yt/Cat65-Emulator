package org.kittykat.cat65.core.cpu;

import org.junit.jupiter.api.BeforeEach;

/**
 * Shared harness for the headless 65c02 core tests: a fresh {@link CPU} on a
 * {@link TestBus} per test, plus helpers to load small programs and step them
 * instruction by instruction — no JavaFX needed.
**/
abstract class CPUTestBase {
    protected static final int START = 0x0200;

    protected static final int FLAG_N = 0x80;
    protected static final int FLAG_V = 0x40;
    protected static final int FLAG_D = 0x08;
    protected static final int FLAG_I = 0x04;
    protected static final int FLAG_Z = 0x02;
    protected static final int FLAG_C = 0x01;

    protected TestBus bus;
    protected CPU cpu;

    @BeforeEach
    void makeCPU() {
        bus = new TestBus();
        cpu = new CPU(bus);
    }

    protected void loadProgram(int... bytes) {
        loadProgramAt(START, bytes);
    }
    protected void loadProgramAt(int address, int... bytes) {
        for (int i = 0; i < bytes.length; i++) {
            bus.memory[(address + i) & 0xffff] = bytes[i] & 0xff;
        }
        bus.memory[0xfffc] = address & 0xff;
        bus.memory[0xfffd] = (address >> 8) & 0xff;
        reset();
    }
    protected int reset() {
        cpu.reset();
        return runToBoundary();
    }
    /**
     * Executes one full instruction (or interrupt sequence) and returns how many clock ticks it took.
    **/
    protected int stepInstruction() {
        cpu.clock();
        return runToBoundary() + 1;
    }
    protected void stepInstructions(int count) {
        for (int i = 0; i < count; i++) {
            stepInstruction();
        }
    }
    protected int runToBoundary() {
        int ticks = 0;
        while (!cpu.isAtInstructionBoundary()) {
            cpu.clock();
            ticks++;
        }
        return ticks;
    }
    protected boolean flag(int mask) {
        return (cpu.getP() & mask) != 0;
    }
}
