package com.agrojurado.sfmappv2.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.agrojurado.sfmappv2.R
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class EvaluacionPdfGenerator(private val context: Context) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun generatePdf(
        evaluaciones: List<EvaluacionPolinizacion>,
        evaluadorMap: Map<Int, String>,
        operarioMap: Map<Int, String>,
        loteMap: Map<Int, String>
    ): File {
        val document = Document(PageSize.A4.rotate())

        val fileName = "evaluaciones_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)

        try {
            PdfWriter.getInstance(document, FileOutputStream(file))
            document.open()

            val subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12f, BaseColor.GRAY)

            // Agregar logo de la empresa
            val logo = BitmapFactory.decodeResource(context.resources, R.drawable.agro_jurado)
            val stream = ByteArrayOutputStream()
            logo.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val logoImage = Image.getInstance(stream.toByteArray())
            logoImage.scaleToFit(100f, 100f)
            logoImage.alignment = Element.ALIGN_LEFT
            document.add(logoImage)

            // Título
            val title = Paragraph(
                "Reporte de Evaluaciones de Polinización",
                Font(Font.FontFamily.HELVETICA, 16f, Font.BOLD)
            )
            title.alignment = Element.ALIGN_CENTER
            title.spacingAfter = 20f
            document.add(title)

            // Fecha de generación
            val dateGenerated = Paragraph(
                "Generado el: ${dateFormat.format(Date())} a las ${timeFormat.format(Date())}",
                subtitleFont
            )
            dateGenerated.alignment = Element.ALIGN_RIGHT
            dateGenerated.spacingAfter = 10f
            document.add(dateGenerated)

            // Tabla
            val table = PdfPTable(18) // Aumentado a 18 columnas para incluir los nuevos campos
            table.widthPercentage = 100f

            // Anchos de columnas actualizados
            val columnWidths = floatArrayOf(
                0.8f, 0.6f, 0.6f, 1.0f, 1.0f, 0.8f, 0.5f, 0.5f, 0.6f, 0.6f, 0.6f,
                0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.8f, 1.2f
            )
            table.setWidths(columnWidths)

            // Encabezados actualizados
            val headers = listOf(
                "Fecha", "Hora", "Semana", "Evaluador", "Polinizador", "Lote",
                "Sección", "Palma", "Inflorescencia", "Antesis", "Post Antesis",
                "Espate", "Aplicación", "Marcación", "Repaso 1", "Repaso 2",
                "Ubicación", "Observaciones"
            )

            val headerFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor.WHITE)
            headers.forEach { header ->
                val cell = PdfPCell(Phrase(header, headerFont))
                cell.backgroundColor = BaseColor(34, 153, 84)
                cell.horizontalAlignment = Element.ALIGN_CENTER
                cell.verticalAlignment = Element.ALIGN_MIDDLE
                cell.setPadding(5f)
                table.addCell(cell)
            }

            // Datos actualizados
            val contentFont = Font(Font.FontFamily.HELVETICA, 9f)
            var rowIndex = 0

            evaluaciones.forEach { evaluacion ->
                rowIndex++
                val isEvenRow = rowIndex % 2 == 0
                val rowColor = if (isEvenRow) BaseColor(245, 245, 245) else BaseColor.WHITE

                val rowData = listOf(
                    evaluacion.fecha ?: "",
                    evaluacion.hora ?: "",
                    evaluacion.semana.toString(),
                    evaluadorMap[evaluacion.idEvaluador] ?: "Desconocido",
                    operarioMap[evaluacion.idPolinizador] ?: "Desconocido",
                    loteMap[evaluacion.idlote] ?: "Desconocido",
                    evaluacion.seccion.toString(),
                    evaluacion.palma.toString(),
                    evaluacion.inflorescencia?.toString() ?: "-",
                    evaluacion.antesis?.toString() ?: "-",
                    evaluacion.postAntesis?.toString() ?: "-",
                    evaluacion.espate?.toString() ?: "-",
                    evaluacion.aplicacion?.toString() ?: "-",
                    evaluacion.marcacion?.toString() ?: "-",
                    evaluacion.repaso1?.toString() ?: "-",
                    evaluacion.repaso2?.toString() ?: "-",
                    evaluacion.ubicacion ?: "",
                    evaluacion.observaciones ?: ""
                )

                rowData.forEach { content ->
                    val cell = PdfPCell(Phrase(content, contentFont))
                    cell.backgroundColor = rowColor
                    cell.horizontalAlignment = Element.ALIGN_CENTER
                    cell.verticalAlignment = Element.ALIGN_MIDDLE
                    cell.setPadding(5f)
                    table.addCell(cell)
                }
            }

            document.add(table)

            // Pie de página
            val footer = Paragraph(
                "Total de registros: ${evaluaciones.size}",
                Font(Font.FontFamily.HELVETICA, 10f, Font.ITALIC)
            )
            footer.spacingBefore = 20f
            footer.alignment = Element.ALIGN_RIGHT
            document.add(footer)

        } catch (e: Exception) {
            throw Exception("Error al generar PDF: ${e.message}")
        } finally {
            document.close()
        }

        return file
    }
}