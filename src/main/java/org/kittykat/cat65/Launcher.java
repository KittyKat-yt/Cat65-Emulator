package org.kittykat.cat65;

import javafx.application.Application;

import java.util.Locale;

/**
 * Separate launcher that does NOT extend javafx.application.Application.
 * <p>
 * When the main class extends Application and is started from the classpath
 * (rather than the module path), the JavaFX runtime aborts with
 * "JavaFX runtime components are missing". Launching through this indirection
 * class avoids that check, so the app runs from both Gradle and IntelliJ
 * without needing --module-path / --add-modules JVM arguments.
 */
public class Launcher {
    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        Application.launch(Cat65.class, args);
    }
}
