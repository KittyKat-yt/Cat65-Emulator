package org.kittykat.cat65.ui.window;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.CacheHint;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.ListViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kittykat.cat65.EmuHelper;
import org.kittykat.cat65.core.CMU;

import java.util.Locale;
import java.util.logging.Logger;

public class HexView extends WindowWithTitle {
    private final ObservableList<String> lines = FXCollections.observableArrayList();
    private final ListView<String> hexView;

    private static final double MAX_FONT_SIZE      = 12d;
    private static final double FONT_SIZE_CONSTANT = MAX_FONT_SIZE / 560d;

    /*
     * this basically just filters out an annoying warning message
     * the thing it's warning about fixes itself, but it still fills the error log
     * (claude wrote this code, but it works, so I'm keeping it)
     */
    private static final Logger CONTROLS_LOGGER = Logger.getLogger("javafx.scene.control");
    static {
        CONTROLS_LOGGER.setFilter(record -> (record.getMessage() == null) || !record.getMessage().contains("index exceeds maxCellCount"));
    }

    public HexView() {
        super("Hex Viewer [CPU Memory]");

        hexView = new ListView<>(lines);
        hexView.setCache(true);
        hexView.setCacheHint(CacheHint.SPEED);
        VBox.setVgrow(hexView, Priority.ALWAYS);

        for (int l = 0x000; l <= 0xfff; l++) {
            lines.add(getVisualizerLine(l));
        }
        getChildren().add(hexView);

        hexView.widthProperty().addListener( (obs, oldWidth, newWidth) -> updateFontSize(newWidth.doubleValue()));
        updateFontSize(hexView.getWidth());
    }

    private void updateFontSize(double width) {
        double newSize = Math.min(MAX_FONT_SIZE, width * FONT_SIZE_CONSTANT);
        hexView.setStyle(String.format(Locale.ROOT, "-fx-font-size: %.3f;", newSize));
    }

    public String getVisualizerLine(int lineNum) {
        int address = lineNum << 4;
        StringBuilder hex = new StringBuilder("$%04x:".formatted(address));
        StringBuilder ascii = new StringBuilder("  |  ");
        for (int a = 0; a < 16; a++) {
            int b = CMU.get(address + a, false);
            hex.append("%s %02x".formatted((a == 8) ? " " : "", b));
            char chr = EmuHelper.convertToASCII(b);
            ascii.append("%s%c".formatted((a == 8) ? " " : "", chr));
        }
        return hex.toString() + ascii;
    }
    public void updateVisualizerLine(int lineNum) {
        lines.set(lineNum, getVisualizerLine(lineNum));
    }

    @Override
    public void updateWindow() {
        ListViewSkin<?> skin = (ListViewSkin<?>) hexView.getSkin();
        VirtualFlow<?> flow = (VirtualFlow<?>) skin.getChildren().getFirst();

        int first = Math.max(0x000, flow.getFirstVisibleCell().getIndex());
        int last  = Math.min(0xfff, flow.getLastVisibleCell().getIndex());

        for (int line = first; line <= last; line++) {
            updateVisualizerLine(line);
        }
    }
}
