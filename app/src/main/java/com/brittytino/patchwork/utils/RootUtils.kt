package com.brittytino.patchwork.utils

import java.io.DataOutputStream
import java.io.IOException

object RootUtils {

    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "which su"))
            // Add a short timeout to prevent hanging the app on problematic devices
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (!process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    return false
                }
            } else {
                // Fallback for older versions (unlikely to be used here but for safety)
                val exitCode = process.waitFor()
                return exitCode == 0
            }
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun isRootPermissionGranted(): Boolean {
        // In many root managers, 'su -c id' will return 0 if granted
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (!process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    return false
                }
            } else {
                val exitCode = process.waitFor()
                return exitCode == 0
            }
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun runCommand(command: String): Boolean {
        var process: Process? = null
        var os: DataOutputStream? = null
        return try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor() == 0
        } catch (e: IOException) {
            false
        } catch (e: InterruptedException) {
            false
        } finally {
            try { os?.close() } catch (e: Exception) {}
            try { process?.destroy() } catch (e: Exception) {}
        }
    }

    fun newProcess(cmd: Array<String>): Process? {
        return try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", cmd.joinToString(" ")))
        } catch (e: Exception) {
            null
        }
    }
}
