package com.example.blocdenotas.data

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.blocdenotas.ui.DrawnPath
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class NotesRepository(context: Context) {
    private val notesDir: File = File(context.filesDir, "saved_notes").apply {
        if (!exists()) mkdirs()
    }

    fun getAllNotes(): List<NoteItem> {
        val notes = mutableListOf<NoteItem>()
        val files = notesDir.listFiles() ?: return emptyList()
        for (file in files) {
            if (file.extension == "json") {
                try {
                    val jsonStr = file.readText()
                    val json = JSONObject(jsonStr)

                    val mathSolutionsArray = json.optJSONArray("mathSolutions") ?: JSONArray()
                    val mathSolutionsList = mutableListOf<String>()
                    for (i in 0 until mathSolutionsArray.length()) {
                        mathSolutionsList.add(mathSolutionsArray.getString(i))
                    }

                    val imageUrisArray = json.optJSONArray("imageUris") ?: JSONArray()
                    val imageUrisList = mutableListOf<String>()
                    for (i in 0 until imageUrisArray.length()) {
                        imageUrisList.add(imageUrisArray.getString(i))
                    }

                    notes.add(
                        NoteItem(
                            id = json.getString("id"),
                            title = json.optString("title", "Nota sin título"),
                            textContent = json.optString("textContent", ""),
                            drawingPathsJson = json.optString("drawingPathsJson", "[]"),
                            imageUris = imageUrisList,
                            mathSolutions = mathSolutionsList,
                            updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return notes.sortedByDescending { it.updatedAt }
    }

    fun saveNote(note: NoteItem) {
        try {
            val file = File(notesDir, "${note.id}.json")
            val json = JSONObject().apply {
                put("id", note.id)
                put("title", note.title.ifBlank { "Nota sin título" })
                put("textContent", note.textContent)
                put("drawingPathsJson", note.drawingPathsJson)

                val imageUrisArray = JSONArray()
                note.imageUris.forEach { imageUrisArray.put(it) }
                put("imageUris", imageUrisArray)

                val mathSolutionsArray = JSONArray()
                note.mathSolutions.forEach { mathSolutionsArray.put(it) }
                put("mathSolutions", mathSolutionsArray)

                put("updatedAt", note.updatedAt)
            }
            file.writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteNote(noteId: String) {
        val file = File(notesDir, "$noteId.json")
        if (file.exists()) {
            file.delete()
        }
    }

    companion object {
        fun drawnPathsToJson(paths: List<DrawnPath>): String {
            val jsonArray = JSONArray()
            for (path in paths) {
                val pathObj = JSONObject()
                pathObj.put("color", path.color.toArgb())
                pathObj.put("strokeWidth", path.strokeWidth.toDouble())
                pathObj.put("isEraser", path.isEraser)
                val pointsArray = JSONArray()
                for (pt in path.points) {
                    val ptObj = JSONObject()
                    ptObj.put("x", pt.x.toDouble())
                    ptObj.put("y", pt.y.toDouble())
                    pointsArray.put(ptObj)
                }
                pathObj.put("points", pointsArray)
                jsonArray.put(pathObj)
            }
            return jsonArray.toString()
        }

        fun jsonToDrawnPaths(jsonStr: String): List<DrawnPath> {
            if (jsonStr.isBlank() || jsonStr == "[]") return emptyList()
            val list = mutableListOf<DrawnPath>()
            try {
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val pathObj = jsonArray.getJSONObject(i)
                    val colorArgb = pathObj.getInt("color")
                    val strokeWidth = pathObj.getDouble("strokeWidth").toFloat()
                    val isEraser = pathObj.optBoolean("isEraser", false)
                    val pointsArray = pathObj.getJSONArray("points")
                    val pointsList = mutableListOf<Offset>()
                    for (j in 0 until pointsArray.length()) {
                        val ptObj = pointsArray.getJSONObject(j)
                        pointsList.add(
                            Offset(
                                ptObj.getDouble("x").toFloat(),
                                ptObj.getDouble("y").toFloat()
                            )
                        )
                    }
                    list.add(
                        DrawnPath(
                            points = pointsList,
                            color = Color(colorArgb),
                            strokeWidth = strokeWidth,
                            isEraser = isEraser
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return list
        }
    }
}
