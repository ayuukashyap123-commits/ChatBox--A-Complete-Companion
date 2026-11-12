package com.chatbox.app;

import com.chatbox.controller.MainController;
import com.chatbox.database.DatabaseManager;
import com.chatbox.service.AiService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Logger;

/**
 * Main JavaFX Application lifecycle coordinator for Chatbox - A Complete Companion.
 */
public class App extends Application {

    private static final Logger LOG = Logger.getLogger(App.class.getName());
    private AiService aiService;

    @Override
    public void init() throws Exception {
        super.init();
        LOG.info("Initializing database connection and schema...");
        DatabaseManager.getInstance().initialize();
        this.aiService = new AiService();
    }

    @Override
    public void start(Stage primaryStage) {
        LOG.info("Starting Chatbox primary stage...");
        MainController mainController = new MainController(primaryStage, aiService);
        Scene scene = new Scene(mainController.build(), 1240, 800);

        primaryStage.setTitle("Chatbox — A Complete Companion");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);

        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        LOG.info("Shutting down Chatbox and cleaning up database connections...");
        DatabaseManager.getInstance().close();
        super.stop();
    }
}
