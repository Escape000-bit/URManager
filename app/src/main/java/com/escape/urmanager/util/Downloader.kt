package com.escape.urmanager.util


import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import java.io.File

/**
 * Hilfsklasse zum Herunterladen von Dateien mit dem DownloadManager.
 */
class Downloader(private val context: Context) {

    interface DownloadListener {
        fun onProgress(message: String)
        fun onFinished(success: Boolean)
    }

    /**
     * Lädt eine Datei von einer URL herunter und speichert sie im gegebenen Zielordner.
     *
     * @param uri Die Download-URL
     * @param destination Das Ziel-Dateiobjekt (z. B. File("/sdcard/Download/test.zip"))
     * @param listener Optionaler Listener für Fortschritt und Status
     * @return true wenn erfolgreich, false sonst
     */
    fun downloadFile(uri: String, destination: File, listener: DownloadListener? = null): Boolean {
        try {
            if (!destination.parentFile.exists()) {
                destination.parentFile.mkdirs()
            }

            val request = DownloadManager.Request(Uri.parse(uri)).apply {
                setTitle("Download: ${destination.name}")
                setDestinationUri(Uri.fromFile(destination))
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)
            listener?.onProgress("📥 Download gestartet: ${destination.name}")

            while (true) {
                val cursor: Cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val totalSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        listener?.onProgress("✅ Download abgeschlossen: ${destination.name}")
                        listener?.onFinished(true)
                        cursor.close()
                        return true
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        listener?.onProgress("❌ Download fehlgeschlagen: ${destination.name}")
                        listener?.onFinished(false)
                        cursor.close()
                        return false
                    }

                    if (totalSize > 0) {
                        val progressPercent = (downloaded * 100 / totalSize).toInt()
                        listener?.onProgress("Fortschritt: $progressPercent")
                    }
                }
                cursor.close()
                Thread.sleep(500)
            }
        } catch (e: Exception) {
            listener?.onProgress("⚠️ Fehler beim Download: ${e.message}")
            listener?.onFinished(false)
            return false
        }
    }

}