package com.example.todoapp;

import business.model.TaskController; // <-- L'import qui manquait
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetSocketAddress;

public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private static final TaskController taskController = new TaskController();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/tasks", taskController::handleTasks);
        server.setExecutor(null);
        server.start();
        log.info("HTTP server started on http://localhost:8080");
    }
}