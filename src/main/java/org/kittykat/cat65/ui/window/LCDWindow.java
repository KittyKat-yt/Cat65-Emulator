package org.kittykat.cat65.ui.window;

import org.kittykat.cat65.core.extraChips.LCD;

public class LCDWindow extends Window {
    @SuppressWarnings({"unused", "FieldCanBeLocal"})  // will be read once the LCD visuals are implemented
    private final LCD lcd;

    public LCDWindow(LCD lcd) {
        super();
        this.lcd = lcd;
        // ToDo: load LCD sprite
    }

    @Override
    public void updateWindow() {
        // ToDo: update visuals
    }
}
