package com.escape.urmanager.util

object RootUtils {
    fun runCommand(cmd: String): Int = try {
        Runtime.getRuntime().exec(arrayOf("su", "-c", cmd)).waitFor()
    } catch (e: Exception) {
        e.printStackTrace()
        -1
    }

    fun getProp(prop: String): String {
        return Runtime.getRuntime().exec(arrayOf("su", "-c", "getprop $prop"))
            .inputStream.bufferedReader().readText().trim()
    }
    fun runCommandWithOutput(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.trim()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
    fun copyAssetToFile(asset: java.io.InputStream, destFile: java.io.File) {
        destFile.outputStream().use { asset.copyTo(it) }
    }
}