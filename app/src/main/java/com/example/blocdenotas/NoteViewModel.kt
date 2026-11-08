package com.example.blocdenotas

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.blocdenotas.data.NoteItem
import com.example.blocdenotas.data.NotesRepository
import com.example.blocdenotas.ui.DrawnPath
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AiState {
    data object Idle : AiState
    data object Loading : AiState
    data class Success(val responseText: String) : AiState
    data class Error(val errorMessage: String) : AiState
}

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NotesRepository(application.applicationContext)

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _notesList = MutableStateFlow<List<NoteItem>>(emptyList())
    val notesList: StateFlow<List<NoteItem>> = _notesList.asStateFlow()

    private val _currentNoteId = MutableStateFlow<String?>(null)
    val currentNoteId: StateFlow<String?> = _currentNoteId.asStateFlow()

    private val _currentTitle = MutableStateFlow("")
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()

    private val _attachedImages = MutableStateFlow<List<String>>(emptyList())
    val attachedImages: StateFlow<List<String>> = _attachedImages.asStateFlow()

    private val _mathSolutions = MutableStateFlow<List<String>>(emptyList())
    val mathSolutions: StateFlow<List<String>> = _mathSolutions.asStateFlow()

    private val _aiState = MutableStateFlow<AiState>(AiState.Idle)
    val aiState: StateFlow<AiState> = _aiState.asStateFlow()

    private val generativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-flash-latest",
            generationConfig = generationConfig {
                temperature = 0.2f
                maxOutputTokens = 400
            }
        )
    }

    init {
        loadNotesList()
    }

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun loadNotesList() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getAllNotes()
            _notesList.value = list
        }
    }

    fun setTitle(title: String) {
        _currentTitle.value = title
    }

    fun addAttachedImage(uriStr: String) {
        _attachedImages.value = _attachedImages.value + uriStr
    }

    fun removeAttachedImage(uriStr: String) {
        _attachedImages.value = _attachedImages.value.filter { it != uriStr }
    }

    fun solveMathFromCanvas(bitmap: Bitmap, customPrompt: String? = null) {
        _aiState.value = AiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val promptText = if (customPrompt.isNullOrBlank()) {
                    """
                    Identifica la operación matemática manuscrita en esta imagen y resuélvela rápidamente.
                    Estructura breve:
                    1. Operación: (operación identificada)
                    2. Resultado: (resultado final destacado)
                    3. Explicación: (máximo 2 líneas breves del procedimiento)
                    """.trimIndent()
                } else {
                    customPrompt
                }

                val response = generativeModel.generateContent(
                    content {
                        image(bitmap)
                        text(promptText)
                    }
                )

                val resultText = response.text
                if (!resultText.isNullOrBlank()) {
                    _aiState.value = AiState.Success(resultText)
                    _mathSolutions.value = _mathSolutions.value + resultText
                } else {
                    _aiState.value = AiState.Error("No se pudo interpretar el dibujo. Traza los números con más claridad.")
                }
            } catch (e: Exception) {
                Log.e("NoteViewModel", "Error en Gemini Canvas Math", e)
                val msg = e.localizedMessage ?: e.message ?: "Error al procesar con IA"
                _aiState.value = AiState.Error("Error en Gemini IA: $msg")
            }
        }
    }

    fun processTextWithAi(textNote: String, actionType: String) {
        val trimmedText = textNote.trim()
        if (trimmedText.isBlank()) {
            _aiState.value = AiState.Error("Escribe texto en la nota antes de consultar a la IA.")
            return
        }
        _aiState.value = AiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val promptText = when (actionType) {
                    "SOLVE" -> "Resuelve rápido estas operaciones y da el resultado directo:\n\n$trimmedText"
                    "SUMMARIZE" -> "Resume brevemente este texto en máximo 3 puntos concisos:\n\n$trimmedText"
                    "IMPROVE" -> "Corrige la ortografía y mejora el formato manteniendo la brevedad:\n\n$trimmedText"
                    "EXPAND" -> "Explica en máximo 3 oraciones breves los conceptos de esta nota:\n\n$trimmedText"
                    "OCR" -> "Extrae y organiza el texto clave contenido en esta nota:\n\n$trimmedText"
                    else -> "Responde rápido y conciso sobre esta nota:\n\n$trimmedText"
                }

                val response = generativeModel.generateContent(
                    content {
                        text(promptText)
                    }
                )

                val resultText = response.text
                if (!resultText.isNullOrBlank()) {
                    _aiState.value = AiState.Success(resultText)
                    if (actionType == "SOLVE") {
                        _mathSolutions.value = _mathSolutions.value + resultText
                    }
                } else {
                    _aiState.value = AiState.Error("La IA no generó respuesta.")
                }
            } catch (e: Exception) {
                Log.e("NoteViewModel", "Error en Gemini Text Action ($actionType)", e)
                val msg = e.localizedMessage ?: e.message ?: "Error de comunicación con IA"
                _aiState.value = AiState.Error("Error en IA: $msg")
            }
        }
    }

    fun saveCurrentNote(textContent: String, drawingPaths: List<DrawnPath>) {
        val noteId = _currentNoteId.value ?: UUID.randomUUID().toString()
        val drawingJson = NotesRepository.drawnPathsToJson(drawingPaths)

        val note = NoteItem(
            id = noteId,
            title = _currentTitle.value.ifBlank { "Nota sin título" },
            textContent = textContent,
            drawingPathsJson = drawingJson,
            imageUris = _attachedImages.value,
            mathSolutions = _mathSolutions.value,
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveNote(note)
            _currentNoteId.value = noteId
            loadNotesList()
        }
    }

    fun loadNote(
        note: NoteItem,
        onNoteLoaded: (textContent: String, drawingPaths: List<DrawnPath>) -> Unit
    ) {
        _currentNoteId.value = note.id
        _currentTitle.value = note.title
        _attachedImages.value = note.imageUris
        _mathSolutions.value = note.mathSolutions
        _aiState.value = AiState.Idle

        val paths = NotesRepository.jsonToDrawnPaths(note.drawingPathsJson)
        onNoteLoaded(note.textContent, paths)
    }

    fun createNewNote(onResetUi: () -> Unit) {
        _currentNoteId.value = null
        _currentTitle.value = ""
        _attachedImages.value = emptyList()
        _mathSolutions.value = emptyList()
        _aiState.value = AiState.Idle
        onResetUi()
    }

    fun deleteNote(noteId: String, onDeletedIfCurrent: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNote(noteId)
            if (_currentNoteId.value == noteId) {
                _currentNoteId.value = null
                _currentTitle.value = ""
                _attachedImages.value = emptyList()
                _mathSolutions.value = emptyList()
                _aiState.value = AiState.Idle
                viewModelScope.launch(Dispatchers.Main) {
                    onDeletedIfCurrent()
                }
            }
            loadNotesList()
        }
    }

    fun resetAiState() {
        _aiState.value = AiState.Idle
    }
}
