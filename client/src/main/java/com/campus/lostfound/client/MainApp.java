package com.campus.lostfound.client;

import com.campus.lostfound.client.util.Navigator;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        Navigator.init(primaryStage);
        Navigator.getInstance().showLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
