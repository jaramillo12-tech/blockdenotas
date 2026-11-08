package com.example.blocdenotas.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blocdenotas.AiState
import com.example.blocdenotas.NoteViewModel
import com.example.blocdenotas.utils.PdfExporter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit,
    onOpenDrawing: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var textNoteState by remember { mutableStateOf(TextFieldValue("")) }

    // Text Undo / Redo history stacks
    val undoStack = remember { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember { mutableStateListOf<TextFieldValue>() }

    fun updateTextWithHistory(newValue: TextFieldValue) {
        if (newValue.text != textNoteState.text) {
            undoStack.add(textNoteState)
            redoStack.clear()
        }
        textNoteState = newValue
    }

    val currentTitle by viewModel.currentTitle.collectAsState()
    val attachedImages by viewModel.attachedImages.collectAsState()
    val mathSolutions by viewModel.mathSolutions.collectAsState()
    val aiState by viewModel.aiState.collectAsState()

    var showAiMenu by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    // Speech Recognizer
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                val currentText = textNoteState.text
                val appended = if (currentText.isBlank()) spokenText else "$currentText\n$spokenText"
                updateTextWithHistory(TextFieldValue(text = appended, selection = TextRange(appended.length)))
            }
        }
    }

    // Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.addAttachedImage(it.toString())
            Toast.makeText(context, "Imagen insertada en el documento", Toast.LENGTH_SHORT).show()
        }
    }

    val mathSymbols = listOf(
        "+", "-", "×", "÷", "=", "√", "π", "x²", "∫", "∑",
        "(", ")", "∞", "≠", "≤", "≥", "±", "^", "θ", "α", "β"
    )

    fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicta tu nota...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Dictado no disponible.", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportToPdf() {
        val imageBitmaps = attachedImages.mapNotNull { uriToBitmap(context, it) }
        val pdfUri = PdfExporter.generateNotePdf(
            context = context,
            title = currentTitle,
            textContent = textNoteState.text,
            drawnBitmap = null,
            imageBitmaps = imageBitmaps,
            mathSolutions = mathSolutions
        )

        if (pdfUri != null) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(pdfUri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(shareIntent, "Compartir PDF"))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Encabezado de la Nota
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveCurrentNote(textNoteState.text, emptyList())
                        onBack()
                    }) {
                        Text("<-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                },
                title = {
                    OutlinedTextField(
                        value = currentTitle,
                        onValueChange = { viewModel.setTitle(it) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        placeholder = { Text("Título", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                },
                actions = {
                    // Deshacer (Undo)
                    IconButton(
                        onClick = {
                            if (undoStack.isNotEmpty()) {
                                val previous = undoStack.removeAt(undoStack.size - 1)
                                redoStack.add(textNoteState)
                                textNoteState = previous
                            }
                        },
                        enabled = undoStack.isNotEmpty()
                    ) {
                        Text("↩", fontSize = 18.sp, color = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                    }
                    // Rehacer (Redo)
                    IconButton(
                        onClick = {
                            if (redoStack.isNotEmpty()) {
                                val next = redoStack.removeAt(redoStack.size - 1)
                                undoStack.add(textNoteState)
                                textNoteState = next
                            }
                        },
                        enabled = redoStack.isNotEmpty()
                    ) {
                        Text("↪", fontSize = 18.sp, color = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                    }
                    // Menú de opciones (Tres puntos)
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Text("...", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Exportar como PDF") },
                                onClick = {
                                    showOptionsMenu = false
                                    exportToPdf()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Compartir nota") },
                                onClick = {
                                    showOptionsMenu = false
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, currentTitle)
                                        putExtra(Intent.EXTRA_TEXT, textNoteState.text)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir Nota"))
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Editor de Texto del Documento
                OutlinedTextField(
                    value = textNoteState,
                    onValueChange = { updateTextWithHistory(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    placeholder = { Text("Escribe aquí tu nota...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                // Chips de Símbolos Matemáticos Rápidos
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    mathSymbols.forEach { symbol ->
                        Surface(
                            onClick = {
                                val currentText = textNoteState.text
                                val selectionStart = textNoteState.selection.start
                                val selectionEnd = textNoteState.selection.end
                                val newText = currentText.substring(0, selectionStart) + symbol + currentText.substring(selectionEnd)
                                updateTextWithHistory(TextFieldValue(newText, selection = TextRange(selectionStart + symbol.length)))
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(symbol, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                // Mostrar Imágenes Adjuntas en el Documento
                if (attachedImages.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Imagenes adjuntas en el documento",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(attachedImages) { uriStr ->
                                val bitmap = uriToBitmap(context, uriStr)
                                if (bitmap != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    ) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Imagen adjunta",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .clickable { viewModel.removeAttachedImage(uriStr) }
                                                .padding(6.dp)
                                        ) {
                                            Text("✕", color = Color.White, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Respuesta de la IA
                AnimatedVisibility(
                    visible = aiState !is AiState.Idle,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Respuesta IA",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(onClick = { viewModel.resetAiState() }) {
                                    Text("✕", fontSize = 12.sp)
                                }
                            }

                            when (val state = aiState) {
                                is AiState.Loading -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Text("Procesando...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                is AiState.Success -> {
                                    Text(
                                        text = state.responseText,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(state.responseText))
                                            },
                                            shape = CircleShape
                                        ) {
                                            Text("Copiar", fontSize = 11.sp)
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = {
                                                val appended = textNoteState.text + "\n\n" + state.responseText
                                                updateTextWithHistory(TextFieldValue(appended, selection = TextRange(appended.length)))
                                            },
                                            shape = CircleShape
                                        ) {
                                            Text("Insertar", fontSize = 11.sp)
                                        }
                                    }
                                }

                                is AiState.Error -> {
                                    Text(
                                        text = "Error: ${state.errorMessage}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                else -> {}
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Barra de Herramientas Flotante Inferior (Dock de Edición de 7 Íconos)
        val dockBg = if (isDarkMode) Color(0xFF1E1E22) else Color(0xFFF2F2F7)
        val dockIconColor = if (isDarkMode) Color.White else Color(0xFF1C1C1E)

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            shape = CircleShape,
            color = dockBg,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Formato de texto ("TT")
                IconButton(onClick = {
                    val text = textNoteState.text
                    val sel = textNoteState.selection
                    val newText = text.substring(0, sel.start) + "**" + text.substring(sel.start, sel.end) + "**" + text.substring(sel.end)
                    updateTextWithHistory(TextFieldValue(newText, selection = TextRange(sel.start + 2, sel.end + 2)))
                }) {
                    Text("TT", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = dockIconColor)
                }

                // 2. Insertar tabla ("Tabla")
                IconButton(onClick = {
                    val text = textNoteState.text
                    val sel = textNoteState.selection.start
                    val tableTemplate = "\n| Col 1 | Col 2 |\n|-------|-------|\n| Val 1 | Val 2 |\n"
                    val newText = text.substring(0, sel) + tableTemplate + text.substring(sel)
                    updateTextWithHistory(TextFieldValue(newText, selection = TextRange(sel + tableTemplate.length)))
                }) {
                    Text("田", fontSize = 18.sp, color = dockIconColor)
                }

                // 3. Insertar imagen
                IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Text("🖼", fontSize = 18.sp, color = dockIconColor)
                }

                // 4. Escaneo de texto ("OCR")
                IconButton(onClick = { viewModel.processTextWithAi(textNoteState.text, "OCR") }) {
                    Text("OCR", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = dockIconColor)
                }

                // 5. Lista de tareas
                IconButton(onClick = {
                    val text = textNoteState.text
                    val sel = textNoteState.selection.start
                    val newText = text.substring(0, sel) + "\n[ ] " + text.substring(sel)
                    updateTextWithHistory(TextFieldValue(newText, selection = TextRange(sel + 5)))
                }) {
                    Text("☑", fontSize = 18.sp, color = dockIconColor)
                }

                // 6. Grabadora de voz
                IconButton(onClick = { startVoiceRecognition() }) {
                    Text("🎤", fontSize = 18.sp, color = dockIconColor)
                }

                // 7. Dibujo a mano alzadas
                IconButton(onClick = { onOpenDrawing() }) {
                    Text("✎", fontSize = 18.sp, color = dockIconColor)
                }
            }
        }
    }
}
