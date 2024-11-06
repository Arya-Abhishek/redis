package command

import cache.RedisCache
import exectuor.CommandExecutor
import java.io.OutputStream
import java.io.PrintWriter

class CommandsHandler(
    private val redisCache: RedisCache
) {
    private val executorMap = hashMapOf<String, CommandExecutor>()

    // registerCommand
    fun registerCommand(command: String, executor: CommandExecutor) {
        executorMap[command] = executor
    }

    // handleCommand
    fun handleCommand(cmds: Command, writer: PrintWriter, outputStream: OutputStream ) {
        this.executorMap.get(cmds.name().uppercase())?.execute(cmds, writer, redisCache, outputStream)
    }
}
