package com.example.blocdenotas.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blocdenotas.NoteViewModel
import com.example.blocdenotas.data.NoteItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: NoteViewModel,
    onOpenNote: (NoteItem?) -> Unit,
    onOpenDrawing: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val notesList by viewModel.notesList.collectAsState()

    var isFabOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val watermarkColor = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val docWidth = 180f
            val docHeight = 240f

            drawRoundRect(
                color = watermarkColor,
                topLeft = Offset(centerX - docWidth / 2f + 20f, centerY - docHeight / 2f - 15f),
                size = Size(docWidth, docHeight),
                cornerRadius = CornerRadius(20f, 20f),
                style = Stroke(width = 4f)
            )
            drawRoundRect(
                color = watermarkColor,
                topLeft = Offset(centerX - docWidth / 2f - 15f, centerY - docHeight / 2f + 15f),
                size = Size(docWidth, docHeight),
                cornerRadius = CornerRadius(20f, 20f),
                style = Stroke(width = 4f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Colecciones",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { }) {
                        LineIconSearch(color = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = { }) {
                        LineIconFilter(color = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        if (isDarkMode) {
                            LineIconSun(color = Color(0xFFF59E0B))
                        } else {
                            LineIconMoon(color = Color(0xFF6366F1))
                        }
                    }
                    IconButton(onClick = { }) {
                        LineIconOptions(color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clickable { onOpenNote(null) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            LineIconPlus(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Text(
                            text = "Crear colección nueva",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (notesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sin colecciones activas",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notesList, key = { it.id }) { note ->
                        DashboardNoteCard(
                            note = note,
                            onSelect = {
                                viewModel.loadNote(note) { _, _ -> }
                                onOpenNote(note)
                            },
                            onDelete = {
                                viewModel.deleteNote(note.id) { }
                            }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 70.dp, end = 20.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedVisibility(
                visible = isFabOpen,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val pillBg = if (isDarkMode) Color(0xFF1F1F24) else Color.White
                    val pillTextColor = if (isDarkMode) Color.White else Color(0xFF1C1C1E)

                    listOf(
                        "Nota" to {
                            isFabOpen = false
                            viewModel.createNewNote { }
                            onOpenNote(null)
                        },
                        "Audio" to {
                            isFabOpen = false
                            onOpenAudio()
                        },
                        "Dibujo" to {
                            isFabOpen = false
                            onOpenDrawing()
                        },
                        "Lista de verificación" to {
                            isFabOpen = false
                            onOpenChecklist()
                        }
                    ).forEach { (label, onClickAction) ->
                        Surface(
                            onClick = onClickAction,
                            shape = CircleShape,
                            color = pillBg,
                            shadowElevation = if (!isDarkMode) 6.dp else 0.dp,
                            modifier = Modifier
                                .height(40.dp)
                                .border(
                                    width = if (isDarkMode) 1.dp else 0.dp,
                                    color = Color.White.copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = pillTextColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                when (label) {
                                    "Nota" -> LineIconPencil(color = pillTextColor)
                                    "Audio" -> LineIconMic(color = pillTextColor)
                                    "Dibujo" -> LineIconBrush(color = pillTextColor)
                                    else -> LineIconCheck(color = pillTextColor)
                                }
                            }
                        }
                    }
                }
            }

            val fabBg = if (isDarkMode) Color.White else Color(0xFF1C1C1E)
            val fabIconColor = if (isDarkMode) Color.Black else Color.White

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(fabBg)
                    .clickable { isFabOpen = !isFabOpen },
                contentAlignment = Alignment.Center
            ) {
                if (isFabOpen) {
                    LineIconClose(color = fabIconColor)
                } else {
                    LineIconPlus(color = fabIconColor)
                }
            }
        }
    }
}

@Composable
fun DashboardNoteCard(
    note: NoteItem,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val dateStr = remember(note.updatedAt) { dateFormat.format(Date(note.updatedAt)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (note.textContent.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = note.textContent.take(50) + if (note.textContent.length > 50) "..." else "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            IconButton(onClick = onDelete) {
                LineIconClose(color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun LineIconSearch(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        drawCircle(color = color, radius = size.width * 0.35f, center = Offset(size.width * 0.45f, size.height * 0.45f), style = Stroke(width = 3f))
        drawLine(color = color, start = Offset(size.width * 0.7f, size.height * 0.7f), end = Offset(size.width * 0.95f, size.height * 0.95f), strokeWidth = 3f, cap = StrokeCap.Round)
    }
}

@Composable
fun LineIconFilter(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        drawLine(color = color, start = Offset(0f, size.height * 0.25f), end = Offset(size.width, size.height * 0.25f), strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(size.width * 0.2f, size.height * 0.5f), end = Offset(size.width * 0.8f, size.height * 0.5f), strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(size.width * 0.4f, size.height * 0.75f), end = Offset(size.width * 0.6f, size.height * 0.75f), strokeWidth = 3f, cap = StrokeCap.Round)
    }
}

@Composable
fun LineIconSun(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        drawCircle(color = color, radius = size.width * 0.25f, center = center, style = Stroke(width = 3f))
        val r = size.width * 0.42f
        for (i in 0 until 8) {
            val angle = i * Math.PI / 4
            val startX = (center.x + (r - 4) * Math.cos(angle)).toFloat()
            val startY = (center.y + (r - 4) * Math.sin(angle)).toFloat()
            val endX = (center.x + r * Math.cos(angle)).toFloat()
            val endY = (center.y + r * Math.sin(angle)).toFloat()
            drawLine(color = color, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 3f, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun LineIconMoon(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.9f, size.height * 0.6f)
            cubicTo(size.width * 0.6f, size.height * 0.9f, size.width * 0.2f, size.height * 0.8f, size.width * 0.1f, size.height * 0.45f)
            cubicTo(size.width * 0.1f, size.height * 0.2f, size.width * 0.3f, 0f, size.width * 0.45f, 0f)
            cubicTo(size.width * 0.35f, size.height * 0.2f, size.width * 0.4f, size.height * 0.5f, size.width * 0.9f, size.height * 0.6f)
            close()
        }
        drawPath(path = path, color = color, style = Stroke(width = 3f))
    }
}

@Composable
fun LineIconOptions(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        drawCircle(color = color, radius = 2.5f, center = Offset(size.width / 2f, size.height * 0.25f))
        drawCircle(color = color, radius = 2.5f, center = Offset(size.width / 2f, size.height * 0.5f))
        drawCircle(color = color, radius = 2.5f, center = Offset(size.width / 2f, size.height * 0.75f))
    }
}

@Composable
fun LineIconPlus(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        drawLine(color = color, start = Offset(size.width / 2f, size.height * 0.2f), end = Offset(size.width / 2f, size.height * 0.8f), strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(size.width * 0.2f, size.height / 2f), end = Offset(size.width * 0.8f, size.height / 2f), strokeWidth = 3f, cap = StrokeCap.Round)
    }
}

@Composable
fun LineIconClose(color: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        drawLine(color = color, start = Offset(size.width * 0.2f, size.height * 0.2f), end = Offset(size.width * 0.8f, size.height * 0.8f), strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(size.width * 0.8f, size.height * 0.2f), end = Offset(size.width * 0.2f, size.height * 0.8f), strokeWidth = 3f, cap = StrokeCap.Round)
    }
}

@Composable
fun LineIconPencil(color: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.7f, size.height * 0.1f)
            lineTo(size.width * 0.9f, size.height * 0.3f)
            lineTo(size.width * 0.3f, size.height * 0.9f)
            lineTo(size.width * 0.1f, size.height * 0.9f)
            lineTo(size.width * 0.1f, size.height * 0.7f)
            close()
        }
        drawPath(path = path, color = color, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun LineIconMic(color: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        drawRoundRect(color = color, topLeft = Offset(size.width * 0.35f, size.height * 0.15f), size = Size(size.width * 0.3f, size.height * 0.5f), cornerRadius = CornerRadius(10f, 10f), style = Stroke(width = 2.5f))
        val path = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.5f)
            cubicTo(size.width * 0.2f, size.height * 0.8f, size.width * 0.8f, size.height * 0.8f, size.width * 0.8f, size.height * 0.5f)
        }
        drawPath(path = path, color = color, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
        drawLine(color = color, start = Offset(size.width * 0.5f, size.height * 0.75f), end = Offset(size.width * 0.5f, size.height * 0.95f), strokeWidth = 2.5f)
    }
}

@Composable
fun LineIconBrush(color: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.8f, size.height * 0.1f)
            lineTo(size.width * 0.9f, size.height * 0.2f)
            lineTo(size.width * 0.4f, size.height * 0.7f)
            lineTo(size.width * 0.2f, size.height * 0.8f)
            close()
        }
        drawPath(path = path, color = color, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun LineIconCheck(color: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        drawCircle(color = color, radius = size.width * 0.42f, center = center, style = Stroke(width = 2.5f))
        val path = Path().apply {
            moveTo(size.width * 0.3f, size.height * 0.5f)
            lineTo(size.width * 0.45f, size.height * 0.65f)
            lineTo(size.width * 0.7f, size.height * 0.35f)
        }
        drawPath(path = path, color = color, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
