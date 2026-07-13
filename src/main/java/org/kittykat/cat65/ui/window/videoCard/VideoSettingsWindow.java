package org.kittykat.cat65.ui.window.videoCard;

import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.kittykat.cat65.Cat65;
import org.kittykat.cat65.ui.window.Window;

public class VideoSettingsWindow extends Window {
    public boolean vSync = true;
    public boolean syncClocks = true;

    public VideoSettingsWindow() {
        super();

        Label lbl_vSync = new Label("V-Sync:");
        CheckBox check_vSync = makeVSyncSetting();

        Label lbl_syncClocks = new Label("Sync Clocks:");
        CheckBox check_syncClocks = makeSyncClocksSetting();

        HBox settingsBox = new HBox(Cat65.SPACING);
        settingsBox.getChildren().addAll(lbl_vSync, check_vSync, new Separator(Orientation.VERTICAL), lbl_syncClocks, check_syncClocks);
        settingsBox.setAlignment(Pos.CENTER_LEFT);

        getChildren().add(settingsBox);
    }

    private CheckBox makeVSyncSetting() {
        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(true);
        checkBox.setOnAction(event -> vSync = checkBox.isSelected());
        checkBox.setTooltip(new Tooltip("""
                makes the display only update at the start of V-Blank
                removes screen-tearing but makes it impossible to do slow-motion debugging with "Sync Clocks"
                """));
        return checkBox;
    }
    private CheckBox makeSyncClocksSetting() {
        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(true);
        checkBox.setOnAction(event -> syncClocks = checkBox.isSelected());
        checkBox.setTooltip(new Tooltip("""
                if enabled, syncs the video pixel clock speed to be proportional to the main CPU clock speed
                this means that the video slows down and speeds up with the CPU, allowing for good slow-motion debugging
                it is recommended that you turn off V-Sync if this is enabled so you can actually see the video signal as it's being generated
                """));
        return checkBox;
    }
}
