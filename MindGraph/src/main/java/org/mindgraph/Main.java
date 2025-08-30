package org.mindgraph;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.awt.*;
import java.awt.event.ActionListener;
import java.net.URL;

public class Main extends Application {
    private TrayIcon trayIcon;

    @Override
    public void start(Stage stage) throws Exception {
        // Load FXML
        FXMLLoader fxml = new FXMLLoader(getClass().getResource("/com/mindgraph/fxml/notepad.fxml"));
        Parent root = fxml.load();
        Scene scene = new Scene(root, 1000, 700);

        // Load CSS
        URL cssUrl = getClass().getResource("/com/mindgraph/css/notepad.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("CSS not found!");
        }

        stage.setTitle("MindGraph Notepad");
        stage.setScene(scene);

        // Minimize to system tray instead of closing
        stage.setOnCloseRequest((WindowEvent event) -> {
            event.consume(); // prevent default close
            hideToSystemTray(stage);
        });

        stage.show();
    }

    private void hideToSystemTray(Stage stage) {
        if (!SystemTray.isSupported()) {
            System.out.println("System tray not supported!");
            stage.hide();
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();

        if (trayIcon != null) return; // already added

        URL iconUrl = getClass().getResource("/com/mindgraph/icons/icon.png");
        if (iconUrl == null) {
            System.err.println("Tray icon not found! Place it under resources/com/mindgraph/icons/");
            stage.hide();
            return;
        }

        Image image = Toolkit.getDefaultToolkit().getImage(iconUrl);

        // Action to show window
        ActionListener showListener = e -> Platform.runLater(() -> {
            stage.show();
            stage.toFront();
            stage.setIconified(false);
            tray.remove(trayIcon);
            trayIcon = null;
        });

        // Action to exit app
        ActionListener exitListener = e -> {
            tray.remove(trayIcon);
            Platform.exit();
            System.exit(0);
        };

        // Popup menu
        PopupMenu popup = new PopupMenu();
        MenuItem openItem = new MenuItem("Open MindGraph");
        openItem.addActionListener(showListener);
        popup.add(openItem);

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(exitListener);
        popup.add(exitItem);

        trayIcon = new TrayIcon(image, "MindGraph", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(showListener);

        try {
            tray.add(trayIcon);
            stage.hide(); // hide window, app stays running
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}