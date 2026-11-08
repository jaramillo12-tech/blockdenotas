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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blocdenotas.AiState
import com.example.blocdenotas.NoteViewModel
import com.example.blocdenotas.data.NoteItem
import com.example.blocdenotas.utils.PdfExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun uriToBitmap(context: Context, uriStr: String): Bitmap? {
    return try {
        val uri = Uri.parse(uriStr)
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    viewModel: NoteViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedModeIndex by rememberSaveable { mutableIntStateOf(0) } // 0: Calculadora Dibujo, 1: Texto Word
    var textNoteState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    val currentTitle by viewModel.currentTitle.collectAsState()
    val notesList by viewModel.notesList.collectAsState()
    val attachedImages by viewModel.attachedImages.collectAsState()
    val mathSolutions by viewModel.mathSolutions.collectAsState()
    val aiState by viewModel.aiState.collectAsState()

    var showNotesSheet by remember { mutableStateOf(false) }
    var showAiMenu by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Drawing states
    val paths = remember { mutableStateListOf<DrawnPath>() }
    val currentPathPoints = remember { mutableStateListOf<Offset>() }
    var currentColor by remember { mutableStateOf(Color(0xFF1E293B)) }
    var currentStrokeWidth by remember { mutableFloatStateOf(6f) }
    var isEraser by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize(0, 0)) }

    // Launcher para reconocimiento de voz (Transcripción)
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                val currentText = textNoteState.text
                val appended = if (currentText.isBlank()) spokenText else "$currentText\n$spokenText"
                textNoteState = TextFieldValue(text = appended, selection = TextRange(appended.length))
            }
        }
    }

    // Launcher para selección de imágenes
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.addAttachedImage(it.toString())
        }
    }

    val mathSymbols = listOf(
        "+", "-", "×", "÷", "=", "√", "π", "x²", "∫", "∑",
        "(", ")", "∞", "≠", "≤", "≥", "±", "^", "θ", "α", "β"
    )

    val colors = listOf(
        Color(0xFF1E293B) to "Oscuro",
        Color(0xFF2563EB) to "Azul",
        Color(0xFFDC2626) to "Rojo",
        Color(0xFF16A34A) to "Verde",
        Color(0xFF9333EA) to "Morado"
    )

    fun resetCanvasAndText() {
        textNoteState = TextFieldValue("")
        paths.clear()
        currentPathPoints.clear()
    }

    fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicta tu nota ahora...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "El dictado por voz no está disponible en este dispositivo.", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportToPdf() {
        val drawnBitmap = if (paths.isNotEmpty() || currentPathPoints.isNotEmpty()) {
            renderPathsToBitmap(
                paths = paths,
                currentPath = currentPathPoints,
                currentColor = currentColor,
                currentStrokeWidth = currentStrokeWidth,
                isEraser = isEraser,
                width = canvasSize.width,
                height = canvasSize.height
            )
        } else null

        val imageBitmaps = attachedImages.mapNotNull { uriToBitmap(context, it) }

        val pdfUri = PdfExporter.generateNotePdf(
            context = context,
            title = currentTitle,
            textContent = textNoteState.text,
            drawnBitmap = drawnBitmap,
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
                // Si no hay visor de PDF, se abre la hoja de compartir
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(shareIntent, "Compartir PDF de la Nota"))
            }
        } else {
            Toast.makeText(context, "No se pudo generar el archivo PDF.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = currentTitle,
                        onValueChange = { viewModel.setTitle(it) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        placeholder = { Text("Título de la nota...", fontSize = 20.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                },
                actions = {
                    OutlinedButton(
                        onClick = { showNotesSheet = true },
                        modifier = Modifier.padding(end = 4.dp),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        Text("Mis Notas", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.createNewNote { resetCanvasAndText() } },
                        modifier = Modifier.padding(end = 4.dp),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        Text("Nueva", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { exportToPdf() },
                        modifier = Modifier.padding(end = 4.dp),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        Text("Exportar PDF", fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.saveCurrentNote(
                        textContent = textNoteState.text,
                        drawingPaths = paths
                    )
                    Toast.makeText(context, "Nota guardada correctamente.", Toast.LENGTH_SHORT).show()
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Pill Segmented Mode Switcher (Neutro y Elegante)
            Surface(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .align(Alignment.CenterHorizontally),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selectedModeIndex == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedModeIndex = 0 }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Lienzo y Calculadora",
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedModeIndex == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selectedModeIndex == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedModeIndex = 1 }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Documento de Texto",
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedModeIndex == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedModeIndex == 0) {
                    // TAB 0: LIENZO Y CALCULADORA DIBUJADA (SIN DELIMITACIONES O BORDES PESADOS)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Paleta de Colores
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                colors.forEach { (color, _) ->
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (!isEraser && currentColor == color) 2.5.dp else 0.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                currentColor = color
                                                isEraser = false
                                            }
                                    )
                                }
                            }

                            // Botón Borrador + Acciones de Trazo
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { isEraser = !isEraser },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = ButtonDefaults.ContentPadding
                                ) {
                                    Text(if (isEraser) "Lapiz" else "Borrador", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (paths.isNotEmpty()) paths.removeAt(paths.size - 1)
                                    },
                                    enabled = paths.isNotEmpty(),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = ButtonDefaults.ContentPadding
                                ) {
                                    Text("Deshacer", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        paths.clear()
                                        currentPathPoints.clear()
                                        viewModel.resetAiState()
                                    },
                                    enabled = paths.isNotEmpty() || currentPathPoints.isNotEmpty(),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = ButtonDefaults.ContentPadding
                                ) {
                                    Text("Limpiar", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Canvas de Dibujo estilo Hoja Digital Sin Borde
                    DrawingCanvas(
                        paths = paths,
                        currentPath = currentPathPoints,
                        currentColor = currentColor,
                        currentStrokeWidth = currentStrokeWidth,
                        isEraser = isEraser,
                        onPathStarted = { offset ->
                            currentPathPoints.clear()
                            currentPathPoints.add(offset)
                        },
                        onPathUpdated = { offset ->
                            currentPathPoints.add(offset)
                        },
                        onPathEnded = {
                            if (currentPathPoints.isNotEmpty()) {
                                paths.add(
                                    DrawnPath(
                                        points = currentPathPoints.toList(),
                                        color = currentColor,
                                        strokeWidth = currentStrokeWidth,
                                        isEraser = isEraser
                                    )
                                )
                                currentPathPoints.clear()
                            }
                        },
                        onSizeChanged = { size ->
                            canvasSize = size
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                    )

                    // Botón para resolver operación en dibujo
                    Button(
                        onClick = {
                            if (paths.isNotEmpty() || currentPathPoints.isNotEmpty()) {
                                val bitmap = renderPathsToBitmap(
                                    paths = paths,
                                    currentPath = currentPathPoints,
                                    currentColor = currentColor,
                                    currentStrokeWidth = currentStrokeWidth,
                                    isEraser = isEraser,
                                    width = canvasSize.width,
                                    height = canvasSize.height
                                )
                                viewModel.solveMathFromCanvas(bitmap)
                            }
                        },
                        enabled = (paths.isNotEmpty() || currentPathPoints.isNotEmpty()) && aiState !is AiState.Loading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Resolver Operación con Inteligencia Artificial", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                } else {
                    // TAB 1: DOCUMENTO DE TEXTO (ESTILO WORD Y HERRAMIENTAS INTEGRADAS)
                    // Barra de Formato Word
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Formato BOLD
                            OutlinedButton(
                                onClick = {
                                    val text = textNoteState.text
                                    val sel = textNoteState.selection
                                    val newText = text.substring(0, sel.start) + "**" + text.substring(sel.start, sel.end) + "**" + text.substring(sel.end)
                                    textNoteState = TextFieldValue(newText, selection = TextRange(sel.start + 2, sel.end + 2))
                                },
                                modifier = Modifier.height(32.dp),
                                contentPadding = ButtonDefaults.ContentPadding
                            ) {
                                Text("B", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            // Formato ITALIC
                            OutlinedButton(
                                onClick = {
                                    val text = textNoteState.text
                                    val sel = textNoteState.selection
                                    val newText = text.substring(0, sel.start) + "*" + text.substring(sel.start, sel.end) + "*" + text.substring(sel.end)
                                    textNoteState = TextFieldValue(newText, selection = TextRange(sel.start + 1, sel.end + 1))
                                },
                                modifier = Modifier.height(32.dp),
                                contentPadding = ButtonDefaults.ContentPadding
                            ) {
                                Text("I", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            }

                            // Formato VIÑETA
                            OutlinedButton(
                                onClick = {
                                    val text = textNoteState.text
                                    val sel = textNoteState.selection.start
                                    val newText = text.substring(0, sel) + "\n- " + text.substring(sel)
                                    textNoteState = TextFieldValue(newText, selection = TextRange(sel + 3))
                                },
                                modifier = Modifier.height(32.dp),
                                contentPadding = ButtonDefaults.ContentPadding
                            ) {
                                Text("Viñeta •", fontSize = 12.sp)
                            }

                            // Transcripción de Voz
                            OutlinedButton(
                                onClick = { startVoiceRecognition() },
                                modifier = Modifier.height(32.dp),
                                contentPadding = ButtonDefaults.ContentPadding
                            ) {
                                Text("Dictar Voz", fontSize = 12.sp)
                            }

                            // Insertar Imagen
                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.height(32.dp),
                                contentPadding = ButtonDefaults.ContentPadding
                            ) {
                                Text("Insertar Imagen", fontSize = 12.sp)
                            }

                            // Asistente IA
                            Box {
                                FilledTonalButton(
                                    onClick = { showAiMenu = true },
                                    enabled = textNoteState.text.isNotBlank() && aiState !is AiState.Loading,
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = ButtonDefaults.ContentPadding
                                ) {
                                    Text("Asistente IA", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                DropdownMenu(
                                    expanded = showAiMenu,
                                    onDismissRequest = { showAiMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Resumir nota") },
                                        onClick = {
                                            showAiMenu = false
                                            viewModel.processTextWithAi(textNoteState.text, "SUMMARIZE")
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Resolver matemáticas") },
                                        onClick = {
                                            showAiMenu = false
                                            viewModel.processTextWithAi(textNoteState.text, "SOLVE")
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Corregir ortografía") },
                                        onClick = {
                                            showAiMenu = false
                                            viewModel.processTextWithAi(textNoteState.text, "IMPROVE")
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Explicar conceptos") },
                                        onClick = {
                                            showAiMenu = false
                                            viewModel.processTextWithAi(textNoteState.text, "EXPAND")
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Campo de Texto estilo Hoja de Documento Sin Delimitación
                    OutlinedTextField(
                        value = textNoteState,
                        onValueChange = { textNoteState = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        placeholder = { Text("Escribe tus notas aquí...") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    // Chips de Símbolos Matemáticos Rápidos
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Símbolos especiales:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                        val newSelectionIndex = selectionStart + symbol.length
                                        textNoteState = TextFieldValue(
                                            text = newText,
                                            selection = TextRange(newSelectionIndex)
                                        )
                                    },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(symbol, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // MOSTRAR IMÁGENES ADJUNTAS
                if (attachedImages.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Imágenes adjuntas:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(attachedImages) { uriStr ->
                                val bitmap = uriToBitmap(context, uriStr)
                                if (bitmap != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
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
                                            Text("✕", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // RESULTADOS DE LA IA
                AnimatedVisibility(
                    visible = aiState !is AiState.Idle,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                                    text = "Respuesta de la Inteligencia Artificial",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                IconButton(onClick = { viewModel.resetAiState() }) {
                                    Text("✕", fontSize = 14.sp)
                                }
                            }

                            when (val state = aiState) {
                                is AiState.Loading -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                                        Text("Procesando consulta...", fontSize = 13.sp)
                                    }
                                }

                                is AiState.Success -> {
                                    Text(
                                        text = state.responseText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(state.responseText))
                                            },
                                            shape = CircleShape
                                        ) {
                                            Text("Copiar", fontSize = 12.sp)
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = {
                                                val appendedText = textNoteState.text + "\n\n--- Resultado IA ---\n" + state.responseText
                                                textNoteState = TextFieldValue(
                                                    text = appendedText,
                                                    selection = TextRange(appendedText.length)
                                                )
                                                selectedModeIndex = 1
                                            },
                                            shape = CircleShape
                                        ) {
                                            Text("Insertar en Nota", fontSize = 12.sp)
                                        }
                                    }
                                }

                                is AiState.Error -> {
                                    Text(
                                        text = "Error: ${state.errorMessage}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                else -> {}
                            }
                        }
                    }
                }

                // HISTORIAL DE SOLUCIONES MATEMÁTICAS EN ESTA NOTA
                if (mathSolutions.isNotEmpty()) {
                    Text(
                        text = "Soluciones guardadas en esta nota:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    mathSolutions.forEachIndexed { idx, solution ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Solución #${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = solution,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // HOJA DESPLEGABLE DE NOTAS GUARDADAS
    if (showNotesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotesSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Mis Notas Guardadas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (notesList.isEmpty()) {
                    Text(
                        text = "No tienes notas guardadas todavía.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notesList, key = { it.id }) { note ->
                            SavedNoteItemCard(
                                note = note,
                                onSelect = {
                                    viewModel.loadNote(note) { text, loadedPaths ->
                                        textNoteState = TextFieldValue(text)
                                        paths.clear()
                                        paths.addAll(loadedPaths)
                                        currentPathPoints.clear()
                                    }
                                    showNotesSheet = false
                                },
                                onDelete = {
                                    viewModel.deleteNote(note.id) {
                                        resetCanvasAndText()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedNoteItemCard(
    note: NoteItem,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val dateStr = remember(note.updatedAt) { dateFormat.format(Date(note.updatedAt)) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(0.85f)) {
                Text(
                    text = note.title.ifBlank { "Nota sin título" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (note.textContent.isNotBlank()) {
                    Text(
                        text = note.textContent.take(50) + if (note.textContent.length > 50) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.height(32.dp),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Text("Eliminar", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
