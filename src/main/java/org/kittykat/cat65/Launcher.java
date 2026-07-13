package org.kittykat.cat65;

import javafx.application.Application;

import java.util.Locale;

public class Launcher {
    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        Application.launch(Cat65.class, args);
    }
}
