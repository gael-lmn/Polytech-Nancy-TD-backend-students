package business.service;

/**
 * DTO retourné au format JSON lors d'une erreur de validation (HTTP 400).
 */
public record ErrorDto(String field, String message) {
}