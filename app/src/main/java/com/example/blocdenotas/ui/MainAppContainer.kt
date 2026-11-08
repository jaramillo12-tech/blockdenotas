package com.example.blocdenotas.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blocdenotas.NoteViewModel
import com.example.blocdenotas.ui.theme.BlocDeNotasTheme

sealed interface Screen {
    data object Dashboard : Screen
    data object NoteEditor : Screen
    data object Drawing : Screen
}

@Composable
fun MainAppContainer(
    viewModel: NoteViewModel = viewModel()
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    BlocDeNotasTheme(darkTheme = isDarkMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                    when (screen) {
                        is Screen.Dashboard -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onOpenNote = { _ ->
                                    currentScreen = Screen.NoteEditor
                                },
                                onOpenDrawing = {
                                    currentScreen = Screen.Drawing
                                },
                                onOpenAudio = {
                                    currentScreen = Screen.NoteEditor
                                },
                                onOpenChecklist = {
                                    currentScreen = Screen.NoteEditor
                                }
                            )
                        }

                        is Screen.NoteEditor -> {
                            NoteEditorScreen(
                                viewModel = viewModel,
                                onBack = {
                                    currentScreen = Screen.Dashboard
                                },
                                onOpenDrawing = {
                                    currentScreen = Screen.Drawing
                                }
                            )
                        }

                        is Screen.Drawing -> {
                            DrawingScreen(
                                viewModel = viewModel,
                                onBack = {
                                    currentScreen = Screen.Dashboard
                                }
                            )
                        }
                    }
                }
            }

            // Bottom Navigation Tabs (Inicio, Editar, Dibujo)
            val borderLineColor = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE5E7EB)
            val activeTabColor = Color(0xFF3B82F6) // Blue 500
            val inactiveTabColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(width = 1.dp, color = borderLineColor),
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tab 1: Inicio
                    Column(
                        modifier = Modifier
                            .clickable { currentScreen = Screen.Dashboard }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val isSelected = currentScreen is Screen.Dashboard
                        LineIconGrid(color = if (isSelected) activeTabColor else inactiveTabColor)
                        Text(
                            text = "Inicio",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) activeTabColor else inactiveTabColor
                        )
                    }

                    // Tab 2: Editar
                    Column(
                        modifier = Modifier
                            .clickable { currentScreen = Screen.NoteEditor }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val isSelected = currentScreen is Screen.NoteEditor
                        LineIconPencil(color = if (isSelected) activeTabColor else inactiveTabColor)
                        Text(
                            text = "Editar",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) activeTabColor else inactiveTabColor
                        )
                    }

                    // Tab 3: Dibujo
                    Column(
                        modifier = Modifier
                            .clickable { currentScreen = Screen.Drawing }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val isSelected = currentScreen is Screen.Drawing
                        LineIconBrush(color = if (isSelected) activeTabColor else inactiveTabColor)
                        Text(
                            text = "Dibujo",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) activeTabColor else inactiveTabColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LineIconGrid(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.padding(1.dp)) {
        val w = size.width
        val h = size.height
        drawRect(color = color, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.42f, h * 0.42f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
        drawRect(color = color, topLeft = androidx.compose.ui.geometry.Offset(w * 0.58f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.42f, h * 0.42f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
        drawRect(color = color, topLeft = androidx.compose.ui.geometry.Offset(0f, h * 0.58f), size = androidx.compose.ui.geometry.Size(w * 0.42f, h * 0.42f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
        drawRect(color = color, topLeft = androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.58f), size = androidx.compose.ui.geometry.Size(w * 0.42f, h * 0.42f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
    }
}
