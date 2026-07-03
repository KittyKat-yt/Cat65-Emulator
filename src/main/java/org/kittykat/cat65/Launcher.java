package org.kittykat.cat65;

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
        Cat65.main(args);
    }
}
