import Cache.RedisCache
import config.RedisConfig
import command.CommandsHandler
import database.RedisDatabase
import exectuor.ConfigCommandExecutor
import exectuor.EchoCommandExecutor
import exectuor.GetCommandExecutor
import exectuor.KeysCommandExecutor
import exectuor.PingCommandExecutor
import exectuor.SetCommandExecutor
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.CRC32

fun parseArgsParams(args: Array<String>): Map<String, String> {
    val argsParamsParsed = mutableMapOf<String, String>()

    var i = 0
    while (i < args.size) {
        if (args[i].startsWith("--")) {
            argsParamsParsed[args[i].substring(2)] = args[i + 1]
            i += 1
        }
        i += 1
    }

    return argsParamsParsed
}

fun main(args: Array<String>) {
    var redisCache = RedisCache()
    val params = parseArgsParams(args)

    val config = HashMap(params)

    var redisConfig = RedisConfig(config)
    val redisDatabases = RedisDatabase(redisConfig)

    // If the RDB database file exists, then read and build the redisCache from it
    redisDatabases.read()
    if (redisDatabases.databases.size > 0) {
        if (redisDatabases.databases.size != 1) {
            throw Exception("Invalid file format")
        }
        if (redisDatabases.databases.size > 1) {
            throw Exception("Invalid file format")
        }
        redisCache = redisDatabases.databases.toList()[0].second
    }

    val commandHandler = CommandsHandler(redisCache)
    commandHandler.registerCommand("PING", PingCommandExecutor())
    commandHandler.registerCommand("KEYS", KeysCommandExecutor())
    commandHandler.registerCommand("ECHO", EchoCommandExecutor())
    commandHandler.registerCommand("SET", SetCommandExecutor())
    commandHandler.registerCommand("GET", GetCommandExecutor())
    commandHandler.registerCommand("CONFIG", ConfigCommandExecutor(config))

    val server = Server(6379, commandHandler)
    server.start()
}
