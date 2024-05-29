/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package simpleWebServer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author user
 */
public class HTTPRequestHandler extends Thread {
    private Socket socket;
    private String logsPath;
    private WebServer webServer;
    
    public HTTPRequestHandler(Socket socket, String logsPath, WebServer server) {
        this.socket = socket;
        this.logsPath = logsPath;
        this.webServer = server;
    }
    
    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            String requestLine = in.readLine();
            if (requestLine == null) return;
            
            String[] tokens = requestLine.split(" ");
            
            if (tokens.length < 2) {
                sendErrorResponse(out, 400, "Bad Request");
                return;
            }
            
            String method = tokens[0];
            String requestURL = tokens[1];
            
            if (method.equals("GET")) {
                serveFile(requestURL, out);
            } else {
                sendErrorResponse(out, 501, "Not Implemented");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
          
    private void serveFile(String requestURL, DataOutputStream out) throws IOException {
        try {
            String filePath = WebServer.getWebRoot() + requestURL.replace("/", "\\");
            File file = new File(filePath);
        
            String clientIP = socket.getInetAddress().getHostAddress();
        
            if (file.exists()) {
                if (file.isDirectory()) {
                    if (requestURL.endsWith("/")) {
                        listDirectory(file, out, getParentDirectory(requestURL));
                        logAccess(requestURL, clientIP, "200 OK");
                    } else {
                        String redirectURL = requestURL + "/";
                        String response = "HTTP/1.1 301 Moved Permanently\r\nLocation: " + redirectURL + "\r\n\r\n";
                        out.writeBytes(response);
//                        logAccess(requestURL, clientIP, "301 Moved Permanently");
                    }
                } else {
                    String contentType = getContentType(file);
                    byte[] fileData = Files.readAllBytes(file.toPath());
                    String response = "HTTP/1.1 200 OK\r\nContent-Length: " + fileData.length +
                                  "\r\nContent-Type: " + contentType + "\r\n\r\n";
                    out.writeBytes(response);
                    out.write(fileData);
                    logAccess(requestURL, clientIP, "200 OK");
                }
            } else {
                sendErrorResponse(out, 404, "Not Found");
                logAccess(requestURL, clientIP, "404 Not Found");
            }
        } catch (IOException e) {
            String errorMessage = e.getMessage();
            String response = "HTTP/1.1 500 Internal Server Error\r\n\r\n";
            out.writeBytes(response);
            logAccess(requestURL, socket.getInetAddress().getHostAddress(), "500 Internal Server Error: " + errorMessage);
        }
    }
    
    private void sendErrorResponse(DataOutputStream out, int statusCode, String message) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + message + "\r\n\r\n";
        out.writeBytes(response);
    }

    private String getContentType(File file) {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        } else if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (fileName.endsWith(".txt")) {
            return "text/plain";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else {
            return "application/octet-stream";
        }
    }
    
    private void listDirectory(File directory, DataOutputStream out, String parentDirectory) throws IOException {
        File[] files = directory.listFiles();
        StringBuilder responseBuilder = new StringBuilder("<html><body><h1><i style=\"color:#F89C0E\"><i style=\"color:#6C78AF\">serverku</i>MyAdmin</i></h1>");

        if (parentDirectory != null) {
            responseBuilder.append("<button onclick=\"goBack()\">Back</button><br><br>");
        }

        responseBuilder.append("<table border=\"1\">");
        responseBuilder.append("<tr><th>Directory List</th></tr>");

        for (File file : files) {
            String fileName = file.getName();
            responseBuilder.append("<tr><td><a href=\"").append(fileName).append("\">").append(fileName).append("</a></td></tr>");
        }
    
        responseBuilder.append("</table>");
        responseBuilder.append("<script>");
        responseBuilder.append("function goBack() { window.history.back(); }");
        responseBuilder.append("</script>");
        responseBuilder.append("</body></html>");

        String response = "HTTP/1.1 200 OK\r\nContent-Length: " + responseBuilder.length() +
                "\r\nContent-Type: text/html\r\n\r\n" + responseBuilder.toString();
        out.writeBytes(response);
    }


    private String getParentDirectory(String requestURL) {
        int lastSlashIndex = requestURL.lastIndexOf("/");
        if (lastSlashIndex > 0) {
            return requestURL.substring(0, lastSlashIndex);
        }
        return null;
    }
        
    private void logAccess(String requestURL, String clientIP, String statusCode) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        String timestamp = formatter.format(new Date());
        String logMessage = String.format("%s [%s] \t %s - %s", timestamp, requestURL, clientIP, statusCode);
        try {
            Files.write(Paths.get(logsPath, "access.log"), (logMessage + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            webServer.logAccess(logMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
