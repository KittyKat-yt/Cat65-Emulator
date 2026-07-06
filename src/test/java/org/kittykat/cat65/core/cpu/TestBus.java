package org.kittykat.cat65.core.cpu;

import org.kittykat.cat65.core.Bus;

/**
 * A minimal headless bus: 64K of flat RAM plus directly controllable IRQ/NMI lines.
**/
class TestBus implements Bus {
    final int[] memory = new int[0x10000];

    /** {@code true} = IRQ asserted (the line itself is active-low, {@link #pollIRQ()} inverts) **/
    boolean irq = false;
    /** one-shot NMI edge, consumed by the next {@link #pollNMI()} **/
    boolean nmi = false;

    @Override
    public int read(int address) {
        return memory[address & 0xffff];
    }
    @Override
    public void write(int address, int value) {
        memory[address & 0xffff] = value & 0xff;
    }

    @Override
    public boolean pollIRQ() {
        return !irq;
    }
    @Override
    public boolean pollNMI() {
        boolean edge = nmi;
        nmi = false;
        return edge;
    }
}
