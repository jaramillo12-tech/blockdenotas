package com.example.blocdenotas.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun generateNotePdf(
        context: Context,
        title: String,
        textContent: String,
        drawnBitmap: Bitmap?,
        imageBitmaps: List<Bitmap>,
        mathSolutions: List<String>
    ): Uri? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // Standard A4 width in points
        val pageHeight = 842 // Standard A4 height in points

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paintText = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }
        val paintTitle = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val paintHeader = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 10f
            isAntiAlias = true
        }
        val paintLine = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        var currentY = 40f
        val marginX = 40f
        val contentWidth = pageWidth - (marginX * 2)

        // Date and Title
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Documento generado: $dateStr", marginX, currentY, paintHeader)
        currentY += 24f

        canvas.drawText(title.ifBlank { "Nota sin título" }, marginX, currentY, paintTitle)
        currentY += 16f

        canvas.drawLine(marginX, currentY, pageWidth - marginX, currentY, paintLine)
        currentY += 24f

        // Draw Text Content
        if (textContent.isNotBlank()) {
            val lines = textContent.split("\n")
            for (line in lines) {
                if (line.isBlank()) {
                    currentY += 12f
                    continue
                }
                val words = line.split(" ")
                var currentLineText = ""

                for (word in words) {
                    val testText = if (currentLineText.isEmpty()) word else "$currentLineText $word"
                    val textWidth = paintText.measureText(testText)

                    if (textWidth > contentWidth) {
                        canvas.drawText(currentLineText, marginX, currentY, paintText)
                        currentY += 18f
                        currentLineText = word

                        if (currentY > pageHeight - 60f) {
                            pdfDocument.finishPage(page)
                            pageNumber++
                            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                            page = pdfDocument.startPage(pageInfo)
                            canvas = page.canvas
                            currentY = 40f
                        }
                    } else {
                        currentLineText = testText
                    }
                }

                if (currentLineText.isNotEmpty()) {
                    canvas.drawText(currentLineText, marginX, currentY, paintText)
                    currentY += 18f
                }
            }
            currentY += 16f
        }

        // Draw Canvas / Math Drawing
        if (drawnBitmap != null) {
            val drawHeight = (contentWidth * (drawnBitmap.height.toFloat() / drawnBitmap.width.toFloat())).coerceAtMost(300f)
            if (currentY + drawHeight > pageHeight - 60f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = 40f
            }

            canvas.drawText("Lienzo de dibujo / Operaciones:", marginX, currentY, paintHeader)
            currentY += 16f

            val srcRect = Rect(0, 0, drawnBitmap.width, drawnBitmap.height)
            val dstRect = Rect(marginX.toInt(), currentY.toInt(), (marginX + contentWidth).toInt(), (currentY + drawHeight).toInt())
            canvas.drawBitmap(drawnBitmap, srcRect, dstRect, null)
            currentY += drawHeight + 24f
        }

        // Draw Attached Images
        for (imgBitmap in imageBitmaps) {
            val imgHeight = (contentWidth * (imgBitmap.height.toFloat() / imgBitmap.width.toFloat())).coerceAtMost(250f)
            if (currentY + imgHeight > pageHeight - 60f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = 40f
            }

            val srcRect = Rect(0, 0, imgBitmap.width, imgBitmap.height)
            val dstRect = Rect(marginX.toInt(), currentY.toInt(), (marginX + contentWidth).toInt(), (currentY + imgHeight).toInt())
            canvas.drawBitmap(imgBitmap, srcRect, dstRect, null)
            currentY += imgHeight + 20f
        }

        // Draw AI Math Solutions
        if (mathSolutions.isNotEmpty()) {
            if (currentY > pageHeight - 100f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = 40f
            }

            canvas.drawText("Resoluciones de la Inteligencia Artificial:", marginX, currentY, paintHeader)
            currentY += 20f

            for ((idx, solution) in mathSolutions.withIndex()) {
                val solTitle = "Solución #${idx + 1}:"
                canvas.drawText(solTitle, marginX, currentY, paintHeader)
                currentY += 16f

                val lines = solution.split("\n")
                for (solLine in lines) {
                    canvas.drawText(solLine, marginX, currentY, paintText)
                    currentY += 16f
                    if (currentY > pageHeight - 60f) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = 40f
                    }
                }
                currentY += 12f
            }
        }

        pdfDocument.finishPage(page)

        return try {
            val pdfDir = File(context.cacheDir, "pdf_exports").apply { if (!exists()) mkdirs() }
            val sanitizeTitle = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "nota" }
            val pdfFile = File(pdfDir, "${sanitizeTitle}_${System.currentTimeMillis()}.pdf")

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }
}
