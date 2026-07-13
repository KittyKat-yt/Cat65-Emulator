package org.kittykat.cat65;

import javafx.application.Application;

import java.util.Locale;

public class Launcher {
    @SuppressWarnings("CallToPrintStackTrace")
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            System.err.printf("[!] Uncaught exception in %s\n", thread.getName());
            exception.printStackTrace();
        });

        Locale.setDefault(Locale.ENGLISH);
        Application.launch(Cat65.class, args);
    }
}
