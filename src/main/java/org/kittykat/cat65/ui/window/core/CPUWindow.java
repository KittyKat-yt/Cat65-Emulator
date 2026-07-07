package org.kittykat.cat65.ui.window.core;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.kittykat.cat65.Cat65;
import org.kittykat.cat65.EmuHelper;
import org.kittykat.cat65.core.CMU;
import org.kittykat.cat65.core.cpu.CPU;
import org.kittykat.cat65.ui.window.WindowWithTitle;

public class CPUWindow extends WindowWithTitle {
    private final CPU cpu;

    private static final String[] STATUS_NAMES = {"A", "X", "Y", "S", "P", "PC", "MDR"};
    private final VBox statusText;

    public CPUWindow(CPU cpu) {
        super("CPU Status");
        this.cpu = cpu;

        statusText = new VBox(Cat65.SPACING);
        for (String statusName : STATUS_NAMES) {
            Label lbl_reg = new Label();
            lbl_reg.setId("reg-%s".formatted(statusName));
            lbl_reg.getStyleClass().add("CPU-reg");
            statusText.getChildren().add(lbl_reg);
        }
        getChildren().add(statusText);
    }

    @Override
    public void updateWindow() {
        for (Node node : statusText.getChildren()) {
            if (node instanceof Label label) {
                String regName = label.getId().substring(4);
                int value = switch (regName) {
                    case "A"   -> cpu.getA();
                    case "X"   -> cpu.getX();
                    case "Y"   -> cpu.getY();
                    case "S"   -> cpu.getS();
                    case "P"   -> cpu.getP();
                    case "PC"  -> cpu.getPC();
                    case "MDR" -> CMU.getMDR();
                    default        -> 0x00;
                };
                if (regName.equals("PC")) {
                    label.setText("%3s:  %5d  |  %04x  |  %s  |  \"%s\"".formatted(regName, value, value,
                            EmuHelper.getBinary(value, true), EmuHelper.getAsciiString(value, true)));
                } else {

                    label.setText("%3s:  %5d  |    %02x  |          %s  |   \"%s\"".formatted(regName, value, value,
                            EmuHelper.getBinary(value, false), EmuHelper.getAsciiString(value, false)));
                }
            }
        }
    }
}
