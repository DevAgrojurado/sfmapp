package com.agrojurado.sfmappv2.utils

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExcelUtils {
    fun Fragment.exportToExcel(
        evaluaciones: List<EvaluacionPolinizacion>,
        evaluadorMap: Map<Int, String>,
        operarioMap: Map<Int, String>,
        loteMap: Map<Int, String>
    ) {
        StoragePermissionHandler(this).requestStoragePermission {
            try {
                // Configurar Apache POI para Android
                System.setProperty(
                    "org.apache.poi.javax.xml.stream.XMLInputFactory",
                    "com.fasterxml.aalto.stax.InputFactoryImpl"
                )
                System.setProperty(
                    "org.apache.poi.javax.xml.stream.XMLOutputFactory",
                    "com.fasterxml.aalto.stax.OutputFactoryImpl"
                )
                System.setProperty(
                    "org.apache.poi.javax.xml.stream.XMLEventFactory",
                    "com.fasterxml.aalto.stax.EventFactoryImpl"
                )

                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Evaluaciones")

                // Crear estilo para encabezados
                val headerStyle = workbook.createCellStyle().apply {
                    fillForegroundColor = IndexedColors.GREY_25_PERCENT.getIndex()
                    fillPattern = FillPatternType.SOLID_FOREGROUND
                    borderBottom = BorderStyle.THIN
                    borderTop = BorderStyle.THIN
                    borderRight = BorderStyle.THIN
                    borderLeft = BorderStyle.THIN
                }

                // Definir encabezados con todos los campos necesarios
                val headers = listOf(
                    "Fecha", "Hora", "Semana", "Evaluador", "Polinizador",
                    "Lote", "Sección", "Palma", "Inflorescencia", "Antesis",
                    "Post-Antesis", "Antesis Dejadas", "Post-Antesis Dejadas",
                    "Espate", "Aplicación", "Marcación",
                    "Repaso 1", "Repaso 2", "Ubicación", "Observaciones"
                )


                // Crear fila de encabezados
                val headerRow = sheet.createRow(0)
                headers.forEachIndexed { index, title ->
                    headerRow.createCell(index).apply {
                        setCellValue(title)
                        cellStyle = headerStyle
                    }
                }

                // Ajustar ancho de columnas
                headers.indices.forEach { i ->
                    sheet.setColumnWidth(i, 15 * 256) // Ajuste de tamaño
                }

                // Llenar datos
                evaluaciones.forEachIndexed { index, evaluacion ->
                    val row = sheet.createRow(index + 1)
                    row.createCell(0).setCellValue(evaluacion.fecha ?: "")
                    row.createCell(1).setCellValue(evaluacion.hora ?: "")
                    row.createCell(2).setCellValue(evaluacion.semana?.toString() ?: "")
                    row.createCell(3).setCellValue(evaluadorMap[evaluacion.idEvaluador] ?: "Desconocido")
                    row.createCell(4).setCellValue(operarioMap[evaluacion.idPolinizador] ?: "Desconocido")
                    row.createCell(5).setCellValue(loteMap[evaluacion.idlote] ?: "Desconocido")
                    row.createCell(6).setCellValue(evaluacion.seccion?.toString() ?: "")
                    row.createCell(7).setCellValue(evaluacion.palma?.toString() ?: "")
                    row.createCell(8).setCellValue(evaluacion.inflorescencia?.toString() ?: "")
                    row.createCell(9).setCellValue(evaluacion.antesis?.toString() ?: "")
                    row.createCell(10).setCellValue(evaluacion.postAntesis?.toString() ?: "")
                    row.createCell(11).setCellValue(evaluacion.antesisDejadas?.toString() ?: "")
                    row.createCell(12).setCellValue(evaluacion.postAntesisDejadas?.toString() ?: "")
                    row.createCell(13).setCellValue(evaluacion.espate?.toString() ?: "")
                    row.createCell(14).setCellValue(evaluacion.aplicacion?.toString() ?: "")
                    row.createCell(15).setCellValue(evaluacion.marcacion?.toString() ?: "")
                    row.createCell(16).setCellValue(evaluacion.repaso1?.toString() ?: "")
                    row.createCell(17).setCellValue(evaluacion.repaso2?.toString() ?: "")
                    row.createCell(18).setCellValue(evaluacion.ubicacion ?: "")
                    row.createCell(19).setCellValue(evaluacion.observaciones ?: "")
                }


                // Generar nombre de archivo con fecha
                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val timestamp = sdf.format(Date())
                val fileName = "Evaluaciones_$timestamp.xlsx"

                // Guardar archivo según la versión de Android
                val outputStream: OutputStream? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = requireContext().contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SFMApp")
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let { resolver.openOutputStream(it) }
                } else {
                    val directory = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "SFMApp"
                    ).apply { mkdirs() }
                    val file = File(directory, fileName)
                    FileOutputStream(file)
                }

                outputStream?.use {
                    workbook.write(it)
                }
                workbook.close()

                Toast.makeText(
                    requireContext(),
                    "Excel guardado en Descargas/SFMApp como $fileName",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Error al exportar Excel: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
