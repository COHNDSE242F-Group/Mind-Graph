package org.mindgraph;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Correct FXML path
        FXMLLoader fxml = new FXMLLoader(getClass().getResource("/com/mindgraph/fxml/notepad.fxml"));
        Parent root = fxml.load();

        Scene scene = new Scene(root, 1000, 700);

        // Correct CSS path (assuming CSS is under resources/com/mindgraph/css/)
        scene.getStylesheets().add(getClass().getResource("/com/mindgraph/css/notepad.css").toExternalForm());

        stage.setTitle("MindGraph Notepad");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}

