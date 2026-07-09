package org.kittykat.cat65.core.cpu;

import org.junit.jupiter.api.BeforeEach;

abstract class CPUTest {
    protected static final int VEC_RESET = 0;
    protected static final int VEC_NMI   = 1;
    protected static final int VEC_IRQ   = 2;

    protected static final int PRG_START = 0x0200;

    protected TestBus bus;
    protected CPU cpu;

    @BeforeEach
    void makeCPU() {
        bus = new TestBus();
        cpu = new CPU(bus);
    }

    protected void loadProgram(int... bytes) {
        loadProgramAt(PRG_START, bytes);
    }
    protected void loadProgramAt(int address, int... bytes) {
        load(address, bytes);
        loadVector(VEC_RESET, address);
        reset();
    }

    protected void load(int address, int... bytes) {
        for (int i = 0; i < bytes.length; i++) {
            bus.memory[(address + i) & 0xffff] = (bytes[i] & 0xff);
        }
    }

    protected void loadStack(int... bytes) {
        int s = cpu.getS();
        for (int i = 0; i < bytes.length; i++) {
            bus.memory[0x0100 | ((s + 1 + i) & 0xff)] = (bytes[i] & 0xff);
        }
    }

    /**
     * @param vector the vector to load<br>0 -> RESET<br>1 -> NMI<br>2 -> IRQ/BRK
     */
    protected void loadVector(int vector, int value) {
        int address = CPU.VECTOR_ADDRESSES[vector];
        load(address, value & 0x00ff, (value & 0xff00) >> 8);
    }

    protected int reset() {
        cpu.reset();
        return runToBoundary();
    }

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
        int cycles = 0;
        while (!cpu.isAtInstructionBoundary()) {
            cpu.clock();
            cycles++;
        }
        return cycles;
    }

    protected boolean flag(char flag) {
        return cpu.getFlag(flag);
    }

    protected int read(int address) {
        return bus.memory[address & 0xffff];
    }

    protected int readStack(int offset) {
        return read(0x0100 | ((cpu.getS() + 1 + offset) & 0xff));
    }
    protected int readStackWord(int offset) {
        return readStack(offset) | (readStack(offset + 1) << 8);
    }
}
