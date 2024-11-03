package exectuor

import Cache.RedisCache
import command.Command
import java.io.PrintWriter

class EchoCommandExecutor: CommandExecutor {
    override fun execute(cmd: Command, writer: PrintWriter, redisCache: RedisCache) {
        val params = cmd.params()
        writer.write("+${params.joinToString("")}\r\n")
        writer.flush()
    }
}
