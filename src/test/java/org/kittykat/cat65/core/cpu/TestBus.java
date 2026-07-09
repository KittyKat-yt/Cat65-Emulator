package org.kittykat.cat65.core.cpu;

class TestBus implements Bus {
    final int[] memory = new int[0x10000];

    public boolean irq = false;
    public boolean nmi = false;

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
