package com.chatbox.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class UIUtil {

    private static final DateTimeFormatter SHORT_FMT = DateTimeFormatter.ofPattern("MMM d, HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");

    public static String formatShort(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(SHORT_FMT);
    }

    public static String formatDate(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DATE_FMT);
    }

    public static String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DATETIME_FMT);
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static Optional<String> showTextInput(String title, String prompt, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(prompt);
        return dialog.showAndWait();
    }

    public static File chooseFile(Stage stage, String title, FileChooser.ExtensionFilter... filters) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        for (FileChooser.ExtensionFilter f : filters) chooser.getExtensionFilters().add(f);
        return chooser.showOpenDialog(stage);
    }

    public static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }

    public static String moodEmoji(int score) {
        return switch (score) {
            case 1, 2 -> "😔";
            case 3, 4 -> "😕";
            case 5, 6 -> "😐";
            case 7, 8 -> "🙂";
            case 9, 10 -> "😊";
            default -> "•";
        };
    }
}
