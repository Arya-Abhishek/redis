package exectuor

import cache.RedisCache
import command.Command
import java.io.OutputStream
import java.io.PrintWriter

interface CommandExecutor {
    /*
    * cmd        - read input command from the stream
    * writer     - output stream writer
    * redisCache - data store cache
    * */
    fun execute(cmd: Command, writer: PrintWriter, redisCache: RedisCache)

    fun execute(cmd: Command, writer: PrintWriter, redisCache: RedisCache, outputStream: OutputStream) {
        execute(cmd, writer, redisCache)
    }
}
