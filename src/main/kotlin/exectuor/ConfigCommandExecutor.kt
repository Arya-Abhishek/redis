package exectuor

import cache.RedisCache
import command.Command
import java.io.PrintWriter

const val GET = "GET"
const val SET = "SET"

class ConfigCommandExecutor(
    private val config: MutableMap<String, String>
): CommandExecutor {
    override fun execute(cmd: Command, writer: PrintWriter, redisCache: RedisCache) {
        val params = cmd.params()
        val subCommand = params[0]
        val key = params[1]

        when (subCommand) {
            GET -> {
                if (config.containsKey(key)) {
                    writer.write("*2\r\n$${key.length}\r\n${key}\r\n")
                    val value = config[key]
                    writer.write("$${value!!.length}\r\n${value}\r\n")
                }
                else
                    writer.write("*0\r\n")
            }
            SET -> {
                val value = params[2]
                config[key] = value
                writer.write("+OK\r\n")
            }
        }

        writer.flush()
    }
}
