package com.chatbox.app;

import javafx.application.Application;

/**
 * Standard JVM entry point that delegates to the JavaFX Application.
 * Having a separate non-Application launcher prevents module-path issues when packaging or running as a standard jar.
 */
public class Main {
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
