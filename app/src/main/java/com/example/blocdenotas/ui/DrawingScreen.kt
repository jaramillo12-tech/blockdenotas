package com.example.blocdenotas.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blocdenotas.AiState
import com.example.blocdenotas.NoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val aiState by viewModel.aiState.collectAsState()

    // Canvas State
    val paths = remember { mutableStateListOf<DrawnPath>() }
    val currentPathPoints = remember { mutableStateListOf<Offset>() }
    var currentColor by remember { mutableStateOf(if (isDarkMode) Color.White else Color.Black) }
    var currentStrokeWidth by remember { mutableFloatStateOf(6f) }
    var isEraser by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize(0, 0)) }

    var isDockVisible by remember { mutableStateOf(true) }
    var activeTool by remember { mutableStateOf("PEN") }
    var showOptionsMenu by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.addAttachedImage(it.toString())
            Toast.makeText(context, "Imagen agregada al dibujo", Toast.LENGTH_SHORT).show()
        }
    }

    val paletteColors = listOf(
        if (isDarkMode) Color.White else Color.Black,
        Color(0xFF2563EB),
        Color(0xFFDC2626),
        Color(0xFF16A34A),
        Color(0xFF9333EA)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Encabezado del Dibujo
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("<-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                },
                title = {
                    Text(
                        text = "Dibujo",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    // Limpiar lienzo
                    OutlinedButton(
                        onClick = {
                            paths.clear()
                            currentPathPoints.clear()
                            viewModel.resetAiState()
                        },
                        modifier = Modifier.padding(end = 4.dp),
                        shape = CircleShape
                    ) {
                        Text("Limpiar", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }

                    // Deshacer
                    IconButton(
                        onClick = { if (paths.isNotEmpty()) paths.removeAt(paths.size - 1) },
                        enabled = paths.isNotEmpty()
                    ) {
                        Text("↩", fontSize = 18.sp, color = if (paths.isNotEmpty()) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                    }

                    // Resolver con IA
                    IconButton(onClick = {
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
                    }) {
                        Text("IA", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                    }

                    // Menú secundario (Tres puntos)
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Text("...", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Opciones de lienzo") },
                                onClick = { showOptionsMenu = false }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            // Canvas de dibujo con fondo adaptativo oscuro en dark mode
            val canvasBg = if (isDarkMode) Color(0xFF0A0A0C) else Color.White
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(canvasBg)
            ) {
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
                    onSizeChanged = { size -> canvasSize = size },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Respuesta IA sobrepuesta
        AnimatedVisibility(
            visible = aiState !is AiState.Idle,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp, start = 20.dp, end = 20.dp)
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Respuesta IA", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                Text("Analizando dibujo...", fontSize = 13.sp)
                            }
                        }
                        is AiState.Success -> {
                            Text(text = state.responseText, fontSize = 13.sp)
                        }
                        is AiState.Error -> {
                            Text(text = "Error: ${state.errorMessage}", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                        }
                        else -> {}
                    }
                }
            }
        }

        // Controles de Dibujo Flotantes
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Botón Ocultar Dock
            Surface(
                onClick = { isDockVisible = !isDockVisible },
                shape = CircleShape,
                color = if (isDarkMode) Color(0xFF1E1E22) else Color(0xFFF2F2F7),
                modifier = Modifier
                    .padding(bottom = 8.dp, end = 12.dp)
                    .size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isDockVisible) "↓" else "↑",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                }
            }

            // Barra de Herramientas de Dibujo Inferior con Variedad de Pinceles
            AnimatedVisibility(
                visible = isDockVisible,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                val dockBg = if (isDarkMode) Color(0xFF1E1E22) else Color(0xFFF2F2F7)
                val dockIconColor = if (isDarkMode) Color.White else Color(0xFF1C1C1E)

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = dockBg,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Paleta de Colores
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            paletteColors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (!isEraser && currentColor == color) 2.dp else 0.dp,
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

                        // Variedad de Pinceles y Herramientas
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Pincel Fino
                            OutlinedButton(
                                onClick = {
                                    activeTool = "FINE"
                                    currentStrokeWidth = 3f
                                    isEraser = false
                                },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Fino", fontSize = 11.sp, fontWeight = if (currentStrokeWidth == 3f && !isEraser) FontWeight.Bold else FontWeight.Normal)
                            }

                            // 2. Pincel Medio
                            OutlinedButton(
                                onClick = {
                                    activeTool = "MEDIUM"
                                    currentStrokeWidth = 8f
                                    isEraser = false
                                },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Medio", fontSize = 11.sp, fontWeight = if (currentStrokeWidth == 8f && !isEraser) FontWeight.Bold else FontWeight.Normal)
                            }

                            // 3. Marcador Grueso
                            OutlinedButton(
                                onClick = {
                                    activeTool = "THICK"
                                    currentStrokeWidth = 18f
                                    isEraser = false
                                },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Grueso", fontSize = 11.sp, fontWeight = if (currentStrokeWidth == 18f && !isEraser) FontWeight.Bold else FontWeight.Normal)
                            }

                            // 4. Borrador
                            OutlinedButton(
                                onClick = {
                                    activeTool = "ERASER"
                                    isEraser = true
                                },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Borrador", fontSize = 11.sp, color = if (isEraser) MaterialTheme.colorScheme.error else dockIconColor)
                            }

                            // 5. Insertar Imagen
                            IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                Text("🖼", fontSize = 16.sp, color = dockIconColor)
                            }
                        }
                    }
                }
            }
        }
    }
}
