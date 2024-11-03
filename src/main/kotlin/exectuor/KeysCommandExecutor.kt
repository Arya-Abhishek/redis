package exectuor

import Cache.RedisCache
import command.Command
import java.io.PrintWriter

class KeysCommandExecutor: CommandExecutor {
    override fun execute(cmd: Command, writer: PrintWriter, redisCache: RedisCache) {
        val params = cmd.params()
        val keys = redisCache.getMatchingKeys(params[0])

        writer.write("*${keys.size}\r\n")
        keys.forEach{it ->
            writer.write("$${it.length}\r\n$it\r\n")
        }
        writer.flush()
    }
}
