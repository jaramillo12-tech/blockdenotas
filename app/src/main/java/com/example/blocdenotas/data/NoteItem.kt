package com.example.blocdenotas.data

import java.util.UUID

data class NoteItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Nota sin título",
    val textContent: String = "",
    val drawingPathsJson: String = "[]",
    val imageUris: List<String> = emptyList(),
    val mathSolutions: List<String> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)
