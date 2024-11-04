package exectuor

import cache.RedisCache
import command.Command
import java.io.PrintWriter

class PingCommandExecutor: CommandExecutor {
    override fun execute(cmd: Command, writer: PrintWriter, redisCache: RedisCache) {
        writer.write("+PONG\r\n")
        writer.flush()
    }
}
