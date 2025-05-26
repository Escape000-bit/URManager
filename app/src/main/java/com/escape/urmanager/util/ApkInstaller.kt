package com.escape.urmanager.util
import java.io.File

class ApkInstaller {
    fun installApk(apk: File): Boolean {
        val cmd = """
            cp "${apk.absolutePath}" "/data/local/tmp/temp.apk" && \
            pm install "/data/local/tmp/temp.apk" && \
            rm "/data/local/tmp/temp.apk"
        """.trimIndent()

        return RootUtils.runCommand(cmd) == 0
    }
}
