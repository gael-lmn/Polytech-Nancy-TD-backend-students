package com.example.todoapp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;

/**
 * Main class of the application. Managing routing and HTTP layer.
 */
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);
    // Expression régulière pour capturer l'ID dans /tasks/123
    private static final Pattern ID_PATH = Pattern.compile("^/tasks/([0-9]+)$");
    private static final TaskDao dao = new TaskDao();

    public static void main(String[] args) throws Exception {
        log.info("In-memory repository initialised");

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/tasks", Application::handleTasks);
        server.setExecutor(null);
        server.start();
        log.info("HTTP server started on http://localhost:8080");
    }

    private static void handleTasks(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            // 1 & 2. Gérer GET /tasks et POST /tasks
            if ("/tasks".equals(path) || "/tasks/".equals(path)) {
                if ("GET".equals(method)) {
                    handleGetTasks(exchange);
                } else if ("POST".equals(method)) {
                    handlePostTask(exchange);
                } else {
                    sendResponse(exchange, 405, null); // Method Not Allowed
                }
                return;
            }

            // 3, 4 & 5. Gérer GET, PUT, DELETE /tasks/{id}
            Matcher m = ID_PATH.matcher(path);
            if (m.matches()) {
                int id = Integer.parseInt(m.group(1));

                if ("GET".equals(method)) {
                    handleGetTaskById(exchange, id);
                } else if ("PUT".equals(method)) {
                    handlePutTask(exchange, id);
                } else if ("DELETE".equals(method)) {
                    handleDeleteTask(exchange, id);
                } else {
                    sendResponse(exchange, 405, null); // Method Not Allowed
                }
                return;
            }

            // Si aucune route ne correspond → 404 Not Found
            sendResponse(exchange, 404, null);

        } catch (Exception e) {
            log.error("Erreur interne du serveur", e);
            sendResponse(exchange, 500, null); // Internal Server Error
        }
    }

    // --- Méthodes de traitement par Endpoint ---

    private static void handleGetTasks(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        boolean todoOnly = query != null && query.contains("todo-only=true");

        List<Task> tasks = dao.findAll(todoOnly);

        if (tasks.isEmpty()) {
            sendResponse(exchange, 204, null); // 204 No Content
        } else {
            sendResponse(exchange, 200, JsonUtils.serialize(tasks));
        }
    }

    private static void handlePostTask(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
        Task input = JsonUtils.deserialize(body, Task.class);
        Task createdTask = dao.save(input);

        exchange.getResponseHeaders().add("Location", "/tasks/" + createdTask.id());
        sendResponse(exchange, 201, JsonUtils.serialize(createdTask));
    }

    private static void handleGetTaskById(HttpExchange exchange, int id) throws IOException {
        Optional<Task> task = dao.findById(id);
        if (task.isPresent()) {
            sendResponse(exchange, 200, JsonUtils.serialize(task.get()));
        } else {
            sendResponse(exchange, 404, null);
        }
    }

    private static void handlePutTask(HttpExchange exchange, int id) throws IOException {
        if (!dao.existsById(id)) {
            sendResponse(exchange, 404, null);
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
        Task input = JsonUtils.deserialize(body, Task.class);

        // On conserve l'ID de l'URL pour s'assurer de modifier la bonne ressource
        Task updatedTask = new Task(id, input.title(), input.description(), input.done());
        dao.save(updatedTask);

        sendResponse(exchange, 204, null); // 204 No Content
    }

    private static void handleDeleteTask(HttpExchange exchange, int id) throws IOException {
        if (dao.deleteById(id)) {
            sendResponse(exchange, 204, null); // 204 No Content
        } else {
            sendResponse(exchange, 404, null);
        }
    }

    // --- Méthode utilitaire pour envoyer la réponse HTTP ---

    private static void sendResponse(HttpExchange exchange, int status, String json) throws IOException {
        if (nonNull(json)) {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = json.getBytes(UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            // Utiliser -1 indique à HttpServer qu'il n'y a pas de corps de réponse
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        }
    }
}