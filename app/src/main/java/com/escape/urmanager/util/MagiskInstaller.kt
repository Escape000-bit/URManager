package com.escape.urmanager.util

import android.widget.TextView
import java.io.File

object MagiskInstaller {
    fun installModule(zip: File, output: TextView) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "magisk --install-module ${zip.absolutePath}"))
            val stdout = process.inputStream.bufferedReader()
            val stderr = process.errorStream.bufferedReader()

            while (process.isAlive) {
                stdout.readLines().forEach { output.post { output.append("\n$it") } }
                stderr.readLines().forEach { output.post { output.append("\n⚠️ $it") } }
                Thread.sleep(300)
            }

            process.waitFor()
        } catch (e: Exception) {
            output.post { output.append("\n⚠️ Fehler bei ${zip.name}: ${e.message}") }
        }
    }
}