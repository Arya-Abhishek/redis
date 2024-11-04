import cache.RedisCache
import config.RedisConfig
import command.CommandsHandler
import config.ReplicationConfig
import database.RedisDatabase
import exectuor.ConfigCommandExecutor
import exectuor.EchoCommandExecutor
import exectuor.GetCommandExecutor
import exectuor.InfoCommandExecutor
import exectuor.KeysCommandExecutor
import exectuor.PingCommandExecutor
import exectuor.ReplconfCommandExecutor
import exectuor.SetCommandExecutor
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

const val SLAVE = "slave"
const val REPLICAOF = "replicaof"

fun parseArgsParams(args: Array<String>, replicationConfig: ReplicationConfig): Map<String, String> {
    val argsParamsParsed = mutableMapOf<String, String>()
    var i = 0
    while (i < args.size) {
        if (args[i].startsWith("--")) {
            argsParamsParsed[args[i].substring(2)] = args[i + 1]
            i += 1
        }
        i += 1
    }
    // Add some default config values
    if (argsParamsParsed.containsKey(REPLICAOF)) {
        replicationConfig.setRole(SLAVE) // else default is master
        val masterConfig = argsParamsParsed[REPLICAOF]!!.split(" ")
        val masterHost = masterConfig[0]
        val masterPort = masterConfig[1]
        replicationConfig.setMasterHost(masterHost)
        replicationConfig.setMasterPort(masterPort.toInt())
    }

    return argsParamsParsed
}

fun establishConnectionWithMaster(replicationConfig: ReplicationConfig, port: Int) {
    try {
        if (replicationConfig.getRole() == SLAVE) {
            val masterHost = replicationConfig.getMasterHost()
            val masterPort = replicationConfig.getMasterPort()

            val socket = Socket(masterHost, masterPort!!)
            val writer = OutputStreamWriter(socket.getOutputStream())
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            // TODO: refactor this, with runCatching and splitting each one into separate functions

            // 1. Send PING command
            writer.write("*1\r\n\$4\r\nPING\r\n")
            writer.flush()
            val pingResponse = reader.readLine()
            if (pingResponse != "+PONG") {
                throw Exception("Failed to connect to master: PING response was $pingResponse")
            }

            // 2. Send REPLCONF listening-port command
            writer.write("*3\r\n\$8\r\nREPLCONF\r\n\$14\r\nlistening-port\r\n\$${port.toString().length}\r\n$port\r\n")
            writer.flush()
            val replconfResponse = reader.readLine()
            if (replconfResponse != "+OK") {
                throw Exception("Failed to connect to master: REPLCONF response was $replconfResponse")
            }

            // 3. Send REPLCONF capa psync2 command
            writer.write("*3\r\n\$8\r\nREPLCONF\r\n\$4\r\ncapa\r\n\$6\r\npsync2\r\n")
            writer.flush()
            val capaResponse = reader.readLine()
            if (capaResponse != "+OK") {
                throw Exception("Failed to connect to master: REPLCONF response was $capaResponse")
            }

            println("Connected to master")
        }
    } catch (e: Exception) {
        throw Exception("Failed to connect to master: ${e.message}")
    }
}

fun main(args: Array<String>) {
    var redisCache = RedisCache()
    val replicationConfig = ReplicationConfig()
    val params = parseArgsParams(args, replicationConfig)
    // Establish connection with master if this is slave instance


    val config = HashMap(params)
    // TODO: can replace these map and class wrapping with getters and setters, with data class
    var redisConfig = RedisConfig(config)   // Just wrapping the config map into RedisConfig object along with getters and setters

    establishConnectionWithMaster(replicationConfig, redisConfig.port())
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
    commandHandler.registerCommand("INFO", InfoCommandExecutor(replicationConfig))
    commandHandler.registerCommand("CONFIG", ConfigCommandExecutor(config))
    commandHandler.registerCommand("REPLCONF", ReplconfCommandExecutor())

    val server = Server(redisConfig.port(), commandHandler)
    server.start()
}
