package com.example.todoapp.business.dto;

public record TaskUpdateRequest(String title, String description, boolean done) {
}