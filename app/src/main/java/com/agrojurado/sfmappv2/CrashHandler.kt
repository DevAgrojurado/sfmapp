package com.agrojurado.sfmappv2

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val LOG_FILE_NAME = "crash_log.txt"

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val writer = StringWriter()
            exception.printStackTrace(PrintWriter(writer))
            val crashDetails = """
                [$timestamp] Crash in thread: ${thread.name}
                Message: ${exception.message}
                StackTrace:
                $writer
                --------------------
            """.trimIndent()

            // Registrar en Logcat
            Log.e("CrashHandler", "Uncaught exception: ${exception.message}", exception)

            // Guardar en la carpeta Descargas
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val logFile = File(downloadsDir, LOG_FILE_NAME)
            logFile.appendText(crashDetails + "\n")

            // Opcional: Enviar a Firebase Crashlytics
            // FirebaseCrashlytics.getInstance().recordException(exception)
        } catch (e: Exception) {
            Log.e("CrashHandler", "Failed to log crash: ${e.message}", e)
        } finally {
            defaultHandler?.uncaughtException(thread, exception)
        }
    }

    fun readCrashLog(): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val logFile = File(downloadsDir, LOG_FILE_NAME)
        return if (logFile.exists()) logFile.readText() else "No crash logs available"
    }

    fun clearCrashLog() {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val logFile = File(downloadsDir, LOG_FILE_NAME)
        if (logFile.exists()) logFile.delete()
    }
}