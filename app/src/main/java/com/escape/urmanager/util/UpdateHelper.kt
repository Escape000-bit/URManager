package com.escape.urmanager.util

import android.content.Context
import android.widget.Toast
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateHelper {
    private const val CURRENT_VERSION = 1.3

    fun checkForUpdate(context: Context, callback: (Boolean) -> Unit) {
        Thread {
            try {
                val url = URL("https://api.github.com/repos/Escape000-bit/URManager/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    val latestVersionTag = jsonObject.getString("tag_name")
                    val latestVersion = latestVersionTag.removePrefix("v").toDoubleOrNull()

                    val updateAvailable = latestVersion != null && latestVersion > CURRENT_VERSION
                    callback(updateAvailable)

                    if (updateAvailable) {

                    }
                } else {
                    callback(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
        }.start()
    }
}
