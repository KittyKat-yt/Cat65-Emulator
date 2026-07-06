package org.kittykat.cat65.core;

/**
 * The CPU's view of the system: the memory map plus the IRQ/NMI lines.<br>
 * {@link CMU} provides the real system bus; tests can plug in a plain 64K array instead.
**/
public interface Bus {
    int read(int address);
    void write(int address, int value);

    /**
     * IRQs are active-low: {@code true} means no interrupt is pending.
    **/
    default boolean pollIRQ() {
        return true;
    }
    /**
     * {@code true} means an NMI edge occurred since the last poll.
    **/
    default boolean pollNMI() {
        return false;
    }
}
