// PdfUtils.kt
package com.agrojurado.sfmappv2.utils

import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.agrojurado.sfmappv2.domain.model.EvaluacionPolinizacion
import java.io.File

object PdfUtils {

    fun Fragment.exportPdf(
        evaluaciones: List<EvaluacionPolinizacion>,
        evaluadorMap: Map<Int, String>,
        operarioMap: Map<Int, String>,
        loteMap: Map<Int, String>
    ) {
        StoragePermissionHandler(this).requestStoragePermission {
            try {
                // Crear directorio en Descargas
                val pdfDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "SFMApp"
                ).apply { mkdirs() }

                // Generar PDF
                val pdfFile = EvaluacionPdfGenerator(requireContext())
                    .generatePdf(evaluaciones, evaluadorMap, operarioMap, loteMap)
                    .let { originalFile ->
                        File(pdfDir, originalFile.name).also { publicFile ->
                            originalFile.copyTo(publicFile, overwrite = true)
                            originalFile.delete()
                        }
                    }

                // Compartir PDF
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    pdfFile
                )

                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Esto es correcto
                }.let {
                    startActivity(Intent.createChooser(it, "Compartir PDF"))
                }

                Toast.makeText(
                    requireContext(),
                    "PDF guardado en Descargas/SFMApp",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
