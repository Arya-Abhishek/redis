package exectuor

import cache.RedisCache
import command.Command
import config.ReplicationConfig
import java.io.PrintWriter

class InfoCommandExecutor(
    private val replicationConfig: ReplicationConfig
): CommandExecutor {

    override fun execute(cmd: Command, writer: PrintWriter, redisCache: RedisCache) {
        val infoString = redisCache.getInfo(replicationConfig)
        writer.write("$${infoString.length}\r\n$infoString\r\n")
        writer.flush()
    }
}
