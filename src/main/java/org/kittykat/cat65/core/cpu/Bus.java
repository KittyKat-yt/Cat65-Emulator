package org.kittykat.cat65.core.cpu;

public interface Bus {
    int  read(int address);
    void write(int address, int value);

    default boolean pollIRQ() {
        return true;
    }
    default boolean pollNMI() {
        return false;
    }
}
