package business.service;

/**
 * Modèle de données interne représentant une tâche.
 */
public record Task(Integer id, String title, String description, boolean done) {
}