/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package simpleWebServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;  

/**
 * 
 *
 * @author user
 */
public class WebServer extends Thread {
    private volatile boolean shouldStop = false;
    private ServerSocket serverSocket;
    private static String webRoot;
    private int port;
    private String logsPath;
    private WebServerUI ui;
    
    public WebServer(String webRoot, String logsPath, int port, WebServerUI ui) {
        WebServer.webRoot = webRoot;
        this.logsPath = logsPath;
        this.port = port;
        this.ui = ui;
    }
    
    public static String getWebRoot() {
        return webRoot;
    }

    public void stopServer() {
        shouldStop = true;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Status change detected: stopped");
                ui.appendLog("Status change detected: stopped");
            } else {
                System.out.println("Server is already stopped");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Status change detected: running");
            ui.appendLog("Status change detected: running");

            while (!shouldStop()) {
                Socket clientSocket = serverSocket.accept();
                HTTPRequestHandler requestHandler = new HTTPRequestHandler(clientSocket, logsPath, this);
                requestHandler.start();
            }
        } catch (IOException e) {
            if (!shouldStop) {
                e.printStackTrace();
//                ui.appendLog("IOException: " + e.getMessage());
                ui.appendLog(e.getMessage());
            }
        } finally {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                    ui.appendLog("Server socket closed.");
                }
            } catch (IOException e) {
                e.printStackTrace();
//                ui.appendLog("IOException while closing server socket: " + e.getMessage());
                ui.appendLog(e.getMessage());
            }
        }
    }

    public boolean shouldStop() {
        return shouldStop;
    }
    
    public void logAccess(String logMessage) {
        ui.appendLog(logMessage);
    }
}
