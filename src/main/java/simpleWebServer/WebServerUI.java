/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package simpleWebServer;

import java.io.File;
import java.util.prefs.Preferences; // 1. Package untuk menyimpan port, filepath, logspath
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 *
 * @author user
 */
public class WebServerUI extends Application {
    private WebServer webServer;
    private TextField filePathField, logsPathField, portField;
    private TextArea logArea;
    private Button toggleButton;
    private final Preferences preferences = Preferences.userNodeForPackage(WebServerUI.class);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SERVERKU Control Panel v1.0.0 [ Compiled: May 20th 2024 ]");
        
        Label titleLabel = new Label("SERVERKU Control Panel v1.0.0");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        HBox titleBox = new HBox(titleLabel);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(20, 0, 20, 0));
        
        BorderPane layout = new BorderPane();
        layout.setTop(titleBox);
        
        Label portLabel = new Label("\tPort\t\t\t");
        portField = new TextField(preferences.get("port", "8080")); // 2. Ketika aplikasi dimulai, program membaca nilai port terakhir yang disimpan dari Preferences dan menampilkannya di portField. [Membaca port dari preferences]
        HBox portBox = new HBox(5, portLabel, portField);
        portBox.setAlignment(Pos.CENTER_LEFT);
        
        Label pathLabel = new Label("\tFile Path\t\t");
        filePathField = new TextField(preferences.get("filePath", "D:\\Web\\Files"));
        Button browseButton = new Button("Browse");
        browseButton.setOnAction(e -> browseFilePath(primaryStage));
        HBox filePathBox = new HBox(5, pathLabel, filePathField, browseButton);
        filePathBox.setAlignment(Pos.CENTER_LEFT);
       
        Label logsPathLabel = new Label("\tLogs Path\t\t");
        logsPathField = new TextField(preferences.get("logsPath", "D:\\Web\\Logs")); 
        Button logsBrowseButton = new Button("Browse");
        logsBrowseButton.setOnAction(e -> browseLogsPath(primaryStage));
        HBox logsPathBox = new HBox(5, logsPathLabel, logsPathField, logsBrowseButton);
        logsPathBox.setAlignment(Pos.CENTER_LEFT);

        toggleButton = new Button("Start");
        toggleButton.setOnAction(e -> toggleWebServer());

        HBox buttonBox = new HBox(10, toggleButton);
        buttonBox.setPadding(new Insets(0, 0, 0, 20));
        buttonBox.setAlignment(Pos.CENTER);

        VBox contentLayout = new VBox(10, portBox, filePathBox, logsPathBox, buttonBox);
        
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(200);
        VBox logBox = new VBox(10, new Label("Access Logs:"), logArea);
        logBox.setPadding(new Insets(20, 20, 20, 20));

        VBox mainLayout = new VBox(10, contentLayout, logBox);
        layout.setCenter(mainLayout);
        
        Scene scene = new Scene(layout, 650, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void browseFilePath(Stage primaryStage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select File Path");
        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory != null) {
            filePathField.setText(selectedDirectory.getAbsolutePath());
        }
    }
    
    private void browseLogsPath(Stage primaryStage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Logs Path");
        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory != null) {
            logsPathField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    private void toggleWebServer() {
        if (webServer == null || !webServer.isAlive()) {
            startWebServer();
        } else {
            stopWebServer();
        }
    }

    private void startWebServer() {
        String filePath = filePathField.getText();
        String logsPath = logsPathField.getText();
        String portText = portField.getText(); // 3. Ketika menekan tombol "Start", aplikasi membaca nilai port dari portField.
        int port;

        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            logArea.appendText("Invalid port number\n");
            return;
        }
        
        preferences.put("port", String.valueOf(port)); // 4.  Memeriksa apakah nilai port valid (angka?). Jika valid, nilai port tersebut disimpan ke Preferences. [Menyimpan nilai port ketika server dimulai]
        preferences.put("filePath", filePath);
        preferences.put("logsPath", logsPath);

        if (webServer == null || !webServer.isAlive()) {
            webServer = new WebServer(filePath, logsPath, port, this);  // 5. Jika server belum berjalan, maka server akan dimulai dengan menggunakan nilai port yang telah disimpan. 
            webServer.start(); // [Memulai server]
            logArea.appendText("Attempting to start SERVERKU app (Port: " + port + ")...\n");
            toggleButton.setText("Stop");
            System.out.println("Attempting to start SERVERKU app (Port: " + port + ")...\n");
        } else {
            logArea.appendText("Server already running\n");
            System.out.println("Server already running");
        }
    }

    private void stopWebServer() {
        String portText = portField.getText();
        int port;

        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            logArea.appendText("Invalid port number\n");
            return;
        }

        preferences.put("port", String.valueOf(port));

        if (webServer != null && webServer.isAlive()) { // 6. Jika menekan tombol "Stop", server akan dihentikan. // [Menghentikan server]
            webServer.stopServer();
            logArea.appendText("Attempting to stop SERVERKU app (Port: " + port + ")...\n");
            toggleButton.setText("Start");
        } else {
            logArea.appendText("Server is not running\n");
        }
    }
  
    public void appendLog(String logMessage) {
        Platform.runLater(() -> logArea.appendText(logMessage + "\n"));
    }
}