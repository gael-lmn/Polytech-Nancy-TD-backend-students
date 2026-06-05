package com.example.todoapp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Data Access Object for {@link Task} model.
 */
public class TaskDao {

    private final Map<Integer, Task> storage = new HashMap<>();

    {
        save(new Task(1, "Réviser DS de maths", "Séries numériques et probabilités.", false));
        save(new Task(2, "Valider mon PIVE", "PIVE Club Poker.", true));
        save(new Task(3, "Choisir mon parcours de 4A", "SIR ou SIA ?", false));
    }

    /**
     * Persist {@link Task} model.
     * @param task task to save.
     * @return task model.
     */
    public Task save(Task task) {
        storage.put(task.id(), task);
        return task;
    }

    /**
     * Retrieve {@link Task} model by id.
     * @param id identifier of the {@link Task}.
     * @return {@link Task} model wrapped by Optional.
     */
    public Optional<Task> findById(int id) {
        return Optional.ofNullable(storage.get(id));
    }

    /**
     * Retrieve all tasks, optionally filtered by 'todo' status.
     * @param todoOnly if true, returns only tasks that are not done.
     * @return List of {@link Task} models.
     */
    public List<Task> findAll(boolean todoOnly) {
        return storage.values().stream()
                .filter(task -> !todoOnly || !task.done())
                .collect(Collectors.toList());
    }

    /**
     * Delete a task by id.
     * @param id identifier of the task.
     * @return true if the task was removed, false otherwise.
     */
    public boolean deleteById(int id) {
        return storage.remove(id) != null;
    }

    /**
     * Check if a task exists by id.
     * @param id identifier of the task.
     * @return true if it exists.
     */
    public boolean existsById(int id) {
        return storage.containsKey(id);
    }
}