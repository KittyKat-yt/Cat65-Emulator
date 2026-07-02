package org.kittykat.cat65.core.extraChips;

import org.kittykat.cat65.core.CMU;

/**
 * I hated this -_-<br>
 * why <b>tf</b> are there two datasheets with different amounts of information???<br>
 * the datasheet for the w65c51<b><i><u>s</u></i></b> documents pretty much all information except for some little bugs
 * because those bugs are only on the w65c51<b><i><u>n</u></i></b>...<br>
 * but the datasheet for the <i>N</i> version does not document <b>nearly</b> as much information for a lot of things!
 * >:c
**/
public class ACIA extends ExtraChip {
    private volatile boolean charAvailable = false;
    private volatile char    crlfQueue     = '\0';
    private volatile char    nextChar      = '\0';

    private int status = 0b0001_0000;
    private int cmd    = 0b0000_0000;
    private int ctrl   = 0b0000_0000;
    private int receiveData = 0x00;

    public ACIA() {
        super(0b0000_0000_0011);
    }

    public void receiveChar(char chr, boolean secondCRLF) {
        // emulator CRLF queue
        if ((nextChar != '\r') && (nextChar != '\n')) {
            crlfQueue = '\0';
        }
        if (secondCRLF) {
            crlfQueue = chr;
        } else {
            nextChar = chr;
        }
        charAvailable = true;
    }

    private void handleReceivedChar() {
        // check if DTR is 1 and the Receiver Clock Source uses the baud rate
        // the external Rx Clock is disconnected so if RCS is 0, the Rx Clock is basically 0 Hz
        if (((cmd & 0b0000_0001) != 0) && ((ctrl &0b0001_0000) != 0)) {
            updateRDR(nextChar);

            // receiver echo mode
            if ((cmd & 0b0001_1100) == 0b0001_0000) {
                CMU.terminalPrint(nextChar);
            }
            update(true);
        }
    }

    private void update(boolean cpu) {
        if (cpu) {
            // check the emulator CRLF queue and if the ACIA is ready for a new byte
            if ((crlfQueue != '\0') && ((status & 0b0000_1000) == 0)) {
                updateRDR(crlfQueue);
                crlfQueue = '\0';
            }

            boolean enabled = ((cmd & 0b0000_0001) != 0);  // if IRQs can be asserted at all
            // determine if RDRF or TDRE can assert IRQs
            boolean rdrf = ((status & 0b0000_1000) != 0) && ((cmd & 0b0000_0010) == 0);
            boolean tdre = ((status & 0b0001_0000) != 0) && ((cmd & 0b0000_1100) == 0b0000_0100);
            // set IRQ flag
            if (enabled && (rdrf || tdre)) {
                status |= 0b1000_0000;
            } else {
                status &= 0b0111_1111;
            }
        }
    }
    private void updateRDR(char chr) {
        receiveData = (chr & 0xff);
        if ((status & 0b0000_1000) != 0) {
            status |= 0b0000_0100;  // overrun
        }
        status |= 0b0000_1000;  // RDR full
    }

    @Override
    public void clock() {
        if (charAvailable) {
            handleReceivedChar();
            charAvailable = false;
        }
    }
    @Override
    public void reset() {
        status &= 0b0110_0000;
        status |= 0b0001_0000;
        ctrl = 0b0000_0000;
        cmd =  0b0000_0000;
    }
    @Override
    public boolean getIRQ() {
        return (status & 0b1000_0000) == 0;
    }

    @Override
    protected int get(int relAddress, boolean cpu) {
        int value = switch (relAddress) {
            case 0b00 -> receiveData;
            case 0b01 -> status;
            case 0b10 -> cmd;
            case 0b11 -> ctrl;
            default   -> CMU.getMDR();
        };
        if (cpu) {
            if (relAddress == 0b00) {
                // clear certain status register bits on RDR read
                status &= 0b1111_0000;
            }
        }
        update(cpu);
        return value;
    }
    @Override
    protected void set(int relAddress, int value, boolean cpu) {
        switch (relAddress) {
            case 0b00 -> {
                // only enable transmitter if DTR is 1
                if ((cmd & 0b0000_0001) != 0) {
                    CMU.terminalPrint((char) value);
                }
            }
            case 0b01 -> {
                // soft reset
                status &= 0b1111_1011;
                cmd &= 0b1110_0000;
            }
            case 0b10 -> cmd = value;
            case 0b11 -> ctrl = value;
        }
        update(cpu);
    }
}
