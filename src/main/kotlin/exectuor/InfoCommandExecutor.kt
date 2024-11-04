package exectuor

import Cache.RedisCache
import command.Command
import java.io.PrintWriter

class InfoCommandExecutor(
    private val configMap: Map<String, String>
): CommandExecutor {
    override fun execute(cmd: Command, writer: PrintWriter, redisCache: RedisCache) {
        val info = redisCache.getInfo(configMap)
        val infoString = info.joinToString("\n")
        writer.write("$${infoString.length}\r\n$infoString\r\n")
        writer.flush()
    }
}
