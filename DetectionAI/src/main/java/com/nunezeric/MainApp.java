package com.nunezeric;

import com.nunezeric.controllers.MainController;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.opencv.core.Core;
import org.opencv.objdetect.CascadeClassifier;

public class MainApp extends Application {


    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("Main.fxml"));
        System.out.println("Fxml loaded");
        BorderPane root = (BorderPane) loader.load();
        Scene scene = new Scene(root,800,500);
        scene.getStylesheets().add("styles.css");
        primaryStage.setTitle("DetectionAI");
        primaryStage.initStyle(StageStyle.UNIFIED);
        primaryStage.setScene(scene);
        primaryStage.show();

        System.out.println("reached primary stage show");
        MainController controller = loader.getController();
        controller.loadClassifier();
        System.out.println("classifier has been loaded");
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                controller.stopApplication();
            }
        });
    }

    public static void main(String[] args) {
        System.out.println("initial test!");
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        launch(args);
    }
}
