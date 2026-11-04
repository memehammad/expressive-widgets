package dev.memehammad.widgets

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

/**
 * Optional shell with ADB rights through Shizuku, for system toggles and battery health.
 * Everything here fails soft: without Shizuku the widgets fall back to opening settings.
 */
object Shell {
    @Volatile
    private var service: IShell? = null

    private val args by lazy {
        Shizuku.UserServiceArgs(ComponentName(BuildConfig.APPLICATION_ID, ShellService::class.java.name))
            .daemon(false)
            .processNameSuffix("shell")
            .debuggable(false)
            .version(BuildConfig.VERSION_CODE)
    }

    fun running(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    fun granted(): Boolean = try {
        running() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    fun requestPermission(code: Int) {
        try {
            Shizuku.requestPermission(code)
        } catch (_: Throwable) {
        }
    }

    private suspend fun connect(): IShell? {
        service?.takeIf { it.asBinder().pingBinder() }?.let { return it }
        if (!granted()) return null
        return withTimeoutOrNull(6000) {
            suspendCancellableCoroutine { cont ->
                val conn = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                        val s = binder?.takeIf { it.pingBinder() }?.let { IShell.Stub.asInterface(it) }
                        service = s
                        if (cont.isActive) cont.resume(s)
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        service = null
                    }
                }
                try {
                    Shizuku.bindUserService(args, conn)
                } catch (e: Throwable) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }

    /** Runs a shell command with ADB rights; null if Shizuku isn't available. */
    suspend fun run(cmd: String): String? = try {
        connect()?.exec(cmd)
    } catch (_: Throwable) {
        service = null
        null
    }
}

/** Shizuku user service: runs as the shell user in its own process. */
class ShellService : IShell.Stub() {
    override fun exec(cmd: String): String = try {
        val p = ProcessBuilder("sh", "-c", cmd).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor()
        out
    } catch (e: Exception) {
        "error: $e"
    }

    override fun destroy() {
        kotlin.system.exitProcess(0)
    }
}
