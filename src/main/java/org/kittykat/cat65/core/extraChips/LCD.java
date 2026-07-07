package org.kittykat.cat65.core.extraChips;

public class LCD {
    public LCD() {
        // ToDo: init CHR-RAM
    }

    /**
     * bits 0-3: Data
    **/
    public int get(int pinState) {
        return 0x00;
    }
    /**
     * bits 0-3: Data<br>
     * bit   4:  RS<br>
     * bit   5:  R/W<br>
     * bit   6:  E
    **/
    public void set(int pinState) {

    }
}
