package exectuor

import Cache.RedisCache
import command.Command
import java.io.PrintWriter

class GetCommandExecutor: CommandExecutor {
    override fun execute(cmd: Command, writer: PrintWriter, redisCache: RedisCache) {
        val cmds = cmd.params()
        val key = cmds[0]
        val cacheValue = redisCache.get(key)
        if (cacheValue == null) {
            writer.write("$-1\r\n")
        } else {
            writer.write("\$${cacheValue.value.length}\r\n${cacheValue.value}\r\n")
        }
        writer.flush()
    }
}
