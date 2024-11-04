package command

import cache.RedisCache
import exectuor.CommandExecutor
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
    fun handleCommand(cmds: Command, writer: PrintWriter) {
        this.executorMap.get(cmds.name().uppercase())?.execute(cmds, writer, redisCache)
    }
}
