package exectuor

import cache.RedisCache
import command.Command
import config.ReplicationConfig
import java.io.OutputStream
import java.io.PrintWriter
import java.time.Instant.*

class SetCommandExecutor(
    private val replicationConfig: ReplicationConfig,
    private val slavesOutputStream: MutableList<OutputStream>
): CommandExecutor {
    override fun execute(cmd: Command, writer: PrintWriter, redisCache: RedisCache) {
        val cmds = cmd.params()
        val key = cmds[0]
        val value = cmds[1]

        var ttl = -1L
        if (cmds.size > 2) {
            when(cmds[2].uppercase()) {
                "EX" -> {
                    ttl = cmds[3].toLong() * 1000
                }
                "PX" -> {
                    ttl = cmds[3].toLong()
                }
            }
            ttl += now().toEpochMilli()
        }

        redisCache.set(key, value,  ttl)
        writer.write("+OK\r\n")
        writer.flush()

        if (replicationConfig.isMaster()) {
            synchronizeSlaves(slavesOutputStream, key, value)
        }
    }

    private fun synchronizeSlaves(slavesOutputStream: MutableList<OutputStream>, key: String, value: String) {
        slavesOutputStream.forEach { outputStream ->
            val cmdStr = "*3\r\n$3\r\nSET\r\n$${key.length}\r\n$key\r\n$${value.length}\r\n$value\r\n"
            outputStream.write(cmdStr.toByteArray(Charsets.UTF_8))
            outputStream.flush()
        }
    }
}
