package com.example.browser.terminal

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Модуль SSH-подключения к VPS.
 * Выполняет команды и возвращает вывод.
 */
object SshClient {
    private var session: Session? = null

    /**
     * Подключается к VPS по SSH с использованием ключа.
     */
    suspend fun connect(
        host: String,
        port: Int,
        user: String,
        keyPath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            val jsch = JSch()
            jsch.addIdentity(keyPath)

            session = jsch.getSession(user, host, port)
            session?.setConfig("StrictHostKeyChecking", "no")
            session?.setConfig("PreferredAuthentications", "publickey")
            session?.connect(10000)
            Result.success("Подключено к $host")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Выполняет команду на VPS и возвращает вывод.
     */
    suspend fun execute(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val s = session ?: return@withContext Result.failure(Exception("Нет подключения"))

            val channel = s.openChannel("exec") as ChannelExec
            channel.setCommand(command)

            val outputStream = ByteArrayOutputStream()
            val errorStream = ByteArrayOutputStream()
            channel.outputStream = outputStream
            channel.errStream = errorStream

            channel.connect(15000)

            // Ждём завершения
            while (!channel.isClosed) {
                Thread.sleep(100)
            }

            val output = outputStream.toString().trim()
            val error = errorStream.toString().trim()
            val exitCode = channel.exitStatus

            channel.disconnect()

            val result = buildString {
                if (output.isNotEmpty()) append(output)
                if (error.isNotEmpty()) {
                    if (isNotEmpty()) append("\n")
                    append("STDERR: $error")
                }
                if (isEmpty()) append("(пустой вывод, код $exitCode)")
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Отключается от VPS.
     */
    fun disconnect() {
        try {
            session?.disconnect()
        } catch (_: Exception) {}
        session = null
    }
}
