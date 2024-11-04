package exectuor

import cache.RedisCache
import command.Command
import java.io.PrintWriter
import java.time.Instant.*

class SetCommandExecutor: CommandExecutor {
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
    }
}
