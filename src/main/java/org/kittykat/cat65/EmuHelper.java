package org.kittykat.cat65;

import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public abstract class EmuHelper {
    public static final URL CSS = loadResource("style.css");

    public static URL loadResource(String path) {
        return EmuHelper.class.getResource("%s%s".formatted(Cat65.RESOURCE_PATH, path));
    }
    public static byte[] readResourceFile(String path) throws IOException {
        try (InputStream in = EmuHelper.class.getResourceAsStream(Cat65.RESOURCE_PATH + path)) {
            if (in == null) {
                throw new FileNotFoundException("\"%s%s\" does not exist!".formatted(Cat65.RESOURCE_PATH, path));
            }
            return in.readAllBytes();
        }
    }

    public static String getBinary(int value, boolean is16bit) {
        value &= 0xffff;
        String bin = String.format("%16s", Integer.toBinaryString(value)).replaceAll(" ", "0");
        return bin.substring(is16bit ? 0 : 8, 16);
    }
    public static String getBinaryString(int value, boolean is16bit) {
        if (is16bit) {
            char cl = convertToASCII( value       & 0xff);
            char ch = convertToASCII((value >> 8) & 0xff);
            return String.valueOf(ch) + cl;
        } else {
            return String.valueOf(convertToASCII(value));
        }
    }

    public static char convertToASCII(int value) {
        char c = (char) value;
        if ((c < 0x20) || (c >= 0x7f)) {
            return (char) 0xb7;
        }
        return c;
    }

    public static int fromBCD(int v) {
        return (v & 0x0f) + (((v & 0xf0) >> 4) * 10);
    }
    public static int toBCD(int v) {
        return (v % 10) + ((v / 10) << 4);
    }

    public static int boolBit(boolean b) {
        return b ? 1 : 0;
    }

    public static int fromTwosComp(int v) {
        return (v & 0x7f) - (v & 0x80);
    }

    public static Button makeButton(String text, EventHandler<ActionEvent> eventHandler) {
        Button btn = new Button(text);
        btn.setOnAction(eventHandler);
        return btn;
    }
    public static <E extends Enum<E>> ComboBox<E> makeSetting(ObjectProperty<E> property, Class<E> enumClass, StringConverter<E> converter) {
        ComboBox<E> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(enumClass.getEnumConstants());
        comboBox.valueProperty().bindBidirectional(property);
        comboBox.setConverter(converter);
        return comboBox;
    }

    public static GridPane makeGrid(int rows, int columns, HPos horizontalAlignment) {
        GridPane grid = new GridPane(Cat65.SPACING, Cat65.SPACING);
        grid.getStyleClass().add("grid-pane");
        grid.setGridLinesVisible(false);
        float rowPercent = 100f / rows;
        for (int r = 0; r < rows; r++) {
            RowConstraints row = new RowConstraints();
            row.setVgrow(Priority.ALWAYS);
            row.setFillHeight(true);
            row.setPercentHeight(rowPercent);
            grid.getRowConstraints().add(row);
        }
        float columnPercent = 100f / columns;
        for (int c = 0; c < columns; c++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setHgrow(Priority.ALWAYS);
            col.setFillWidth(true);
            col.setPercentWidth(columnPercent);
            col.setHalignment(horizontalAlignment);
            grid.getColumnConstraints().add(col);
        }
        return grid;
    }

    public static NumberAxis makeNumberAxis(double min, double max, double tickUnit) {
        NumberAxis axis = new NumberAxis();
        axis.setAutoRanging(false);
        axis.setLowerBound(min);
        axis.setUpperBound(max);
        axis.setTickUnit(tickUnit);
        return axis;
    }

    public static void applyCSS(Scene scene) {
        if (CSS != null) {
            scene.getStylesheets().add(CSS.toExternalForm());
        } else {
            System.err.println("[!] Could not load the stylesheet...");
        }
    }
}
