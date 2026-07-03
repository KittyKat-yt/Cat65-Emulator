package org.kittykat.cat65;

import javafx.scene.layout.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.kittykat.cat65.core.CMU;
import org.kittykat.cat65.core.ExpansionData;

public class Cat65 extends Application {
    public static final String RESOURCE_PATH = "/org/kittykat/cat65/";

    public static final int    SAMPLE_RATE = 48_000;
    public static final double SAMPLE_PERIOD_NANOS = 1_000_000_000d / SAMPLE_RATE;
    public static final float  SPACING = 8f;

    @Override
    public void stop() {
        CMU.stop();
    }

    @Override
    public void start(Stage mainStage) {
        ExpansionData.init();

        BorderPane root = new BorderPane();
        root.setId("root");
        root.setCenter(CMU.makeWindow());

        Scene scene = new Scene(root, 1000, 750);
        EmuHelper.applyCSS(scene);

        mainStage.setTitle("Cat65 Emulator >w<");
        mainStage.setScene(scene);
        mainStage.setMaximized(true);
        mainStage.setOnCloseRequest(event -> stop());
        mainStage.show();
        CMU.startThreads();
    }
}
