package business.model;

import business.JsonUtils;
import com.sun.net.httpserver.HttpExchange;
import business.service.TaskService;
import business.service.ErrorDto;

// <-- LES BONS IMPORTS POUR TES DTOs -->
import com.example.todoapp.business.dto.TaskCreateRequest;
import com.example.todoapp.business.dto.TaskResponse;
import com.example.todoapp.business.dto.TaskUpdateRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;

public class TaskController {
    private static final Logger log = LoggerFactory.getLogger(TaskController.class);
    private static final Pattern ID_PATH = Pattern.compile("^/tasks/([0-9]+)$");
    private final TaskService taskService = new TaskService();

    public void handleTasks(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            log.info("Requête interceptée par TaskController : {} {}", method, path);

            // GET /tasks/count
            if ("GET".equals(method) && "/tasks/count".equals(path)) {
                int total = taskService.countTasks();
                sendResponse(exchange, 200, String.valueOf(total));
                return;
            }

            // GET /tasks
            if ("GET".equals(method) && "/tasks".equals(path)) {
                boolean todoOnly = nonNull(query) && query.contains("todoOnly=true");
                Collection<TaskResponse> tasks = taskService.getAllTasks(todoOnly);
                sendResponse(exchange, 200, JsonUtils.serialize(tasks));
                return;
            }

            // POST /tasks
            if ("POST".equals(method) && "/tasks".equals(path)) {
                TaskCreateRequest input = JsonUtils.deserialize(
                        new String(exchange.getRequestBody().readAllBytes(), UTF_8),
                        TaskCreateRequest.class
                );
                if (input.title() == null || input.title().trim().isEmpty()) {
                    sendValidationError(exchange, "title", "Le titre est obligatoire.");
                    return;
                }
                if (input.title().length() > 50) {
                    sendValidationError(exchange, "title", "Le titre ne peut pas dépasser 50 caractères.");
                    return;
                }
                if (input.description() != null && input.description().length() > 255) {
                    sendValidationError(exchange, "description", "La description ne peut pas dépasser 255 caractères.");
                    return;
                }
                TaskResponse created = taskService.createTask(input);
                exchange.getResponseHeaders().add("Location", "/tasks/" + created.id());
                sendResponse(exchange, 201, JsonUtils.serialize(created));
                return;
            }

            // DELETE /tasks
            if ("DELETE".equals(method) && "/tasks".equals(path)) {
                taskService.deleteAllTasks();
                sendResponse(exchange, 204, null);
                return;
            }

            Matcher m = ID_PATH.matcher(path);
            if (m.matches()) {
                int id = Integer.parseInt(m.group(1));

                // GET /tasks/{id}
                if ("GET".equals(method)) {
                    Optional<TaskResponse> task = taskService.getTaskById(id);
                    if (task.isPresent()) {
                        sendResponse(exchange, 200, JsonUtils.serialize(task.get()));
                    } else {
                        sendResponse(exchange, 404, null);
                    }
                    return;
                }

                // DELETE /tasks/{id}
                if ("DELETE".equals(method)) {
                    boolean deleted = taskService.deleteTaskById(id);
                    if (deleted) {
                        sendResponse(exchange, 204, null);
                    } else {
                        sendResponse(exchange, 404, null);
                    }
                    return;
                }

                // PUT /tasks/{id}
                if ("PUT".equals(method)) {
                    TaskUpdateRequest input = JsonUtils.deserialize(
                            new String(exchange.getRequestBody().readAllBytes(), UTF_8),
                            TaskUpdateRequest.class
                    );
                    if (input.title() == null || input.title().trim().isEmpty()) {
                        sendValidationError(exchange, "title", "Le titre est obligatoire.");
                        return;
                    }
                    if (input.title().length() > 50) {
                        sendValidationError(exchange, "title", "Le titre ne peut pas dépasser 50 caractères.");
                        return;
                    }
                    if (input.description() != null && input.description().length() > 255) {
                        sendValidationError(exchange, "description", "La description ne peut pas dépasser 255 caractères.");
                        return;
                    }
                    boolean updated = taskService.updateTask(id, input);
                    if (updated) {
                        sendResponse(exchange, 204, null);
                    } else {
                        sendResponse(exchange, 404, null);
                    }
                    return;
                }
            }
            sendResponse(exchange, 404, null);
        } catch (Exception e) {
            log.error("Erreur serveur interne capturée :", e);
            try {
                sendResponse(exchange, 500, null);
            } catch (IOException ioEx) {
                log.error("Impossible d'envoyer le code HTTP 500", ioEx);
            }
        }
    }

    private void sendValidationError(HttpExchange exchange, String field, String message) throws IOException {
        ErrorDto error = new ErrorDto(field, message);
        sendResponse(exchange, 400, JsonUtils.serialize(error));
    }

    private void sendResponse(HttpExchange exchange, int status, String json) throws IOException {
        if (nonNull(json)) {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = json.getBytes(UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        }
    }
}