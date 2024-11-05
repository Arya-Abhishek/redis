package exectuor

import cache.RedisCache
import command.Command
import java.io.PrintWriter

const val LISTENING_PORT = "listening-port"
const val CAPA = "capa"

class ReplconfCommandExecutor: CommandExecutor {
    override fun execute(cmd: Command, writer: PrintWriter, redisCache: RedisCache) {
        val subCommandName = cmd.subCommand()
        val subCommands = cmd.params()

        when (subCommandName) {
            LISTENING_PORT -> {
                // do something, with subCommands list containing port 6380 , listening port of slave
            }
            CAPA -> {
                // do something, with subCommands = ["eof", "capa"]
            }
            else -> {
                throw Exception("Invalid sub command for replication configuration")
            }
        }

        // response in both cases will be OK in RESP simple string
        writer.write("+OK\r\n")
        writer.flush()
    }
}
