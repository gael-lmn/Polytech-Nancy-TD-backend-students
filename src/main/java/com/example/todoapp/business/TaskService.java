package business.service;

// <-- LES BONS IMPORTS POUR TES DTOs -->
import com.example.todoapp.business.dto.TaskCreateRequest;
import com.example.todoapp.business.dto.TaskResponse;
import com.example.todoapp.business.dto.TaskUpdateRequest;

import dao.SQLiteDAO;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class TaskService {

    private final SQLiteDAO sqliteDao = new SQLiteDAO();

    public TaskResponse createTask(TaskCreateRequest request) {
        Task task = new Task(null, request.title(), request.description(), false);
        Task savedTask = sqliteDao.save(task);
        return mapToResponse(savedTask);
    }

    public Optional<TaskResponse> getTaskById(int id) {
        return sqliteDao.findById(id).map(this::mapToResponse);
    }

    public Collection<TaskResponse> getAllTasks(boolean todoOnly) {
        return sqliteDao.findAll(todoOnly).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public boolean updateTask(int id, TaskUpdateRequest request) {
        Optional<Task> existing = sqliteDao.findById(id);
        if (existing.isPresent()) {
            Task updatedTask = new Task(id, request.title(), request.description(), request.done());
            sqliteDao.save(updatedTask);
            return true;
        }
        return false;
    }

    public boolean deleteTaskById(int id) {
        return sqliteDao.deleteById(id);
    }

    public void deleteAllTasks() {
        sqliteDao.clearAll();
    }

    public int countTasks() {
        return sqliteDao.count();
    }

    private TaskResponse mapToResponse(Task task) {
        return new TaskResponse(task.id(), task.title(), task.description(), task.done());
    }
}