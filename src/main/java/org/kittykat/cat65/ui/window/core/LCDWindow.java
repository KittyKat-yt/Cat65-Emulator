package org.kittykat.cat65.ui.window.core;

import org.kittykat.cat65.core.extraChips.LCD;
import org.kittykat.cat65.ui.window.Window;

public class LCDWindow extends Window {
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
