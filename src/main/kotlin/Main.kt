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

fun establishConnectionWithMaster(replicationConfig: ReplicationConfig) {
    try {
        if (replicationConfig.getRole() == SLAVE) {
            val masterHost = replicationConfig.getMasterHost()
            val masterPort = replicationConfig.getMasterPort()

            val socket = Socket(masterHost, masterPort!!)
            val writer = OutputStreamWriter(socket.getOutputStream())
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            // Send PING command
            writer.write("*1\r\n\$4\r\nPING\r\n")
            writer.flush()
            val pingResponse = reader.readLine()
            if (pingResponse != "+PONG") {
                throw Exception("Failed to connect to master: PING response was $pingResponse")
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
    establishConnectionWithMaster(replicationConfig)

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
    commandHandler.registerCommand("INFO", InfoCommandExecutor(replicationConfig))
    commandHandler.registerCommand("CONFIG", ConfigCommandExecutor(config))

    val server = Server(redisConfig.port(), commandHandler)
    server.start()
}
