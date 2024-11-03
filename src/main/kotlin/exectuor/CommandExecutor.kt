package exectuor

import Cache.RedisCache
import command.Command
import java.io.PrintWriter

interface CommandExecutor {
    /*
    * cmd        - read input command from the stream
    * writer     - output stream writer
    * redisCache - data store cache
    * */
    fun execute(cmd: Command, writer: PrintWriter, redisCache: RedisCache)
}
