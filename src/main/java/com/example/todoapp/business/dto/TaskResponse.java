package com.example.todoapp.business.dto;

public record TaskResponse(Integer id, String title, String description, boolean done) {
}