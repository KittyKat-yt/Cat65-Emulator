package org.kittykat.cat65.ui.window;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.HPos;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.StringConverter;
import org.kittykat.cat65.EmuHelper;
import org.kittykat.cat65.core.CMU;
import org.kittykat.cat65.settings.ExpansionPort;
import org.kittykat.cat65.settings.NewLineVariant;

import java.io.File;

import static org.kittykat.cat65.EmuHelper.makeButton;
import static org.kittykat.cat65.EmuHelper.makeSetting;

public class ConfigWindow extends Window {

    private final FileChooser romChooser = new FileChooser();

    public final ObjectProperty<NewLineVariant> newLineVariant = new SimpleObjectProperty<>(NewLineVariant.CR);
    public final ObjectProperty<ExpansionPort>  exPort04       = new SimpleObjectProperty<>(ExpansionPort._Disconnected);
    public final ObjectProperty<ExpansionPort>  exPort07       = new SimpleObjectProperty<>(ExpansionPort._Disconnected);

    // 1kHz -> 1ms = 1_000_000ns
    // 1MHz -> 1µs = 1_000ns
    public static final long DEFAULT_CLOCK_PERIOD = 1_000L;  // 1MHz
    public volatile long clockPeriodNanos = DEFAULT_CLOCK_PERIOD;

    public ConfigWindow() {
        super();
        GridPane controls = initControls();
        GridPane settings = initSettings();
        getChildren().addAll(controls, new Separator(), settings);
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    private GridPane initControls() {
        Button btn_load            = initRomChooser();
        Button btn_reset           = makeButton("Reset", event -> CMU.reset = true);
        Button btn_instructionStep = makeButton("Step",  event -> CMU.step  = true);

        Label lbl_paused = new Label("Paused");
        Button btn_pause = new Button("Resume");
        btn_pause.setOnAction(event -> {
            btn_instructionStep.setDisable(CMU.paused);
            CMU.paused = !CMU.paused;
            lbl_paused.setVisible(CMU.paused);
            btn_pause.setText(CMU.paused ? "Resume" : "Pause");
        });

        Button btn_clearTerminal = makeButton("Clear Screen", event -> CMU.clearTerminal());
        Button btn_showDebug =     makeButton("Toggle Debug", event -> CMU.toggleDebugMode());

        GridPane controls = EmuHelper.makeGrid(2, 5, HPos.CENTER);
        controls.addRow(0, btn_load,  lbl_paused);
        controls.addRow(1, btn_reset, btn_pause, btn_instructionStep);
        controls.add(btn_clearTerminal, 3, 0, 2, 1);
        controls.add(btn_showDebug,     3, 1, 2, 1);
        return controls;
    }
    private Button initRomChooser() {
        romChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        romChooser.setTitle("Open ROM :3c");
        romChooser.getExtensionFilters().addAll(
                new ExtensionFilter("ROMs", "*.c65", "*.rom", "*.bin"),
                new ExtensionFilter("all files", "*")
        );
        return makeButton("Load ROM", event -> {
            File file = romChooser.showOpenDialog(getScene().getWindow());
            if (file != null) {
                CMU.loadROM(file);
            }
        });
    }

    private GridPane initSettings() {
        Label lbl_newLineSetting = new Label("New Line Variant:");
        ComboBox<NewLineVariant> setting_newLine    = makeSetting(newLineVariant, NewLineVariant.class, NewLineVariant.stringConverter);

        GridPane settings = EmuHelper.makeGrid(4, 3, HPos.LEFT);
        settings.addRow(0, lbl_newLineSetting, setting_newLine);
        initExpansionConfig(settings);
        initClockSpinner(settings);
        return settings;
    }
    private void initExpansionConfig(GridPane settings) {
        Button btn_openPort04 = makeButton("Open Window ($4)", event -> CMU.openEx04Window());
        Button btn_openPort07 = makeButton("Open Window ($7)", event -> CMU.openEx07Window());
        btn_openPort04.setDisable(true);
        btn_openPort07.setDisable(true);

        Label lbl_expansionPorts = new Label("ExpansionPorts:");
        ComboBox<ExpansionPort>  setting_expansion4 = makeSetting(exPort04,       ExpansionPort.class,  ExpansionPort.stringConverter);
        ComboBox<ExpansionPort>  setting_expansion7 = makeSetting(exPort07,       ExpansionPort.class,  ExpansionPort.stringConverter);
        exPort04.addListener((obs, oldVal, newVal) -> {
            CMU.swapExpansion04(newVal);
            btn_openPort04.setDisable(CMU.doesEx04HaveNoWindow());
        });
        exPort07.addListener((obs, oldVal, newVal) -> {
            CMU.swapExpansion07(newVal);
            btn_openPort07.setDisable(CMU.doesEx07HaveNoWindow());
        });

        settings.addRow(1, lbl_expansionPorts, setting_expansion4, setting_expansion7);
        settings.add(btn_openPort04, 1, 2);
        settings.add(btn_openPort07, 2, 2);
    }
    private void initClockSpinner(GridPane settings) {
        Label lbl_clockPeriod             = new Label("Clock Period [ns]:");
        Spinner<Integer> clockPeriodInput = new Spinner<>(850, 100_000_000, (int) DEFAULT_CLOCK_PERIOD, 1);
        clockPeriodInput.setEditable(true);

        IntegerSpinnerValueFactory factory = (IntegerSpinnerValueFactory) clockPeriodInput.getValueFactory();
        StringConverter<Integer> def = factory.getConverter();
        factory.setConverter(new StringConverter<>() {
            @Override public String toString(Integer v) {
                return def.toString(v);
            }
            @Override public Integer fromString(String s) {
                try {
                    return def.fromString(s);
                } catch (NumberFormatException e) {
                    clockPeriodInput.cancelEdit();
                    return factory.getValue();
                }
            }
        });

        clockPeriodInput.valueProperty().addListener((obs, oldVal, newVal) -> clockPeriodNanos = newVal);
        clockPeriodInput.focusedProperty().addListener((obs, was, is) -> {
            if (!is) clockPeriodInput.commitValue();
        });

        settings.addRow(3, lbl_clockPeriod,    clockPeriodInput);
    }
}
