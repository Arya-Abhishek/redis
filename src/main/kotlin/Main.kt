import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    val redisDataStore = mutableMapOf<String, String>()
    val redisKeyExpiry = mutableMapOf<String, Long>()
    val redisConfigStore = mutableMapOf<String, String>()

    // RDB Persistence
    createRdbPersistence(args, redisConfigStore)

     val serverSocket = ServerSocket(6379)
     serverSocket.reuseAddress = true
     println("Server is running on port 6379")

     while (true) {
          val clientSocket = serverSocket.accept()

            Thread {

                val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                val writer = OutputStreamWriter(clientSocket.getOutputStream())

                try {
                    while (true) {
                        val commandList = parseInput(reader)
                        if (commandList.isEmpty()) break
                        val commandType = commandList[0].trim().uppercase()
                        var resp = ""
                        when (commandType) {
                            "PING" -> {
                                commandList.forEach {
                                    if (it.trim().uppercase() == "PING") {
                                        resp += "+PONG\r\n"
                                    }
                                }
                            }

                            "ECHO" -> {
                                for (i in 1..<commandList.size) {
                                    if (commandList[i] in listOf("", "\n", "\r\n")) continue
                                    resp += "$${commandList[i].length}\r\n${commandList[i]}\r\n"
                                }
                            }

                            "SET" -> {
                                val key = commandList[1] // get the key
                                val value = commandList[2] // get the value
                                if (commandList.size > 3 && commandList[3].uppercase() == "PX") {
                                    val expiryTime = commandList[4].toLong() // get the expiry time
                                    redisDataStore[key] = value // set the value in redis
                                    redisKeyExpiry[key] = System.currentTimeMillis() + expiryTime // set the expiry time
                                } else {
                                    redisDataStore[key] = value // set the value in redis
                                }
                                resp = "+OK\r\n"
                            }

                            "GET" -> {
                                val key = commandList[1]
                                val value = getValue(key, redisDataStore, redisKeyExpiry)
                                resp = if (value.isNotEmpty()){ "\$${value.length}\r\n${value}\r\n" } else "\$-1\r\n"
                            }

                            "CONFIG" -> {
                                if (commandList.size > 1 && commandList[1].uppercase() == "GET") {
                                    val configKey = commandList[2]
                                    resp = "*2\r\n"
                                    when (configKey) {
                                        "dir" -> {
                                            val dir = redisConfigStore["dir"] ?: ""
                                            resp += "\$${"dir".length}\r\ndir\r\n\$${dir.length}\r\n${dir}\r\n"
                                        }
                                        "dbfilename" -> {
                                            val dbFilename = redisConfigStore["dbFilename"] ?: ""
                                            resp += "\$${"dbfilename".length}\r\ndbfilename\r\n\$${dbFilename.length}\r\n${dbFilename}\r\n"
                                        }
                                        else -> {
                                            resp = "-ERR unknown CONFIG parameter '$configKey'\r\n"
                                        }
                                    }
                                } else { // config set
                                    resp = "-ERR CONFIG subcommand must be one of GET\r\n"
                                }
                            }

                            else -> {
                                resp = "-ERR unknown command '$commandType'\r\n"
                            }
                        }

                        writer.write(resp)
                        writer.flush()
                    }
                } catch (e: Exception) {
                    println("Error: $e")
                } finally {
                    clientSocket.close()
                }
            }.start()
     }
}

fun parseInput(reader: BufferedReader): List<String> {
    val commandList = mutableListOf<String>()
    val numberOfCommands = reader.readLine() ?: return emptyList() // will give, *2, *3, etc
    val numberOfCommandsInt = numberOfCommands.substring(1).toInt() ?: 0

    for (i in 0 until numberOfCommandsInt) {
        reader.readLine() // will give, $3, $4, etc
        val command = reader.readLine()    // will give, SET, GET, etc
        commandList.add(command)
    }

    return commandList
}

fun getValue(key: String, redisDataStore: MutableMap<String, String>, redisKeyExpiry: MutableMap<String, Long>): String {
    val redisValue = redisDataStore[key] ?: return ""
    if (redisKeyExpiry.containsKey(key) && redisKeyExpiry[key] != null) {
        if (redisKeyExpiry[key]!! < System.currentTimeMillis()) {
            redisDataStore.remove(key)
            return ""
        }
    }

    return redisValue
}

data class RedisValue(
    val value: String,
    val expiryTimeMillis: Long?,
    val lastSetTimeMillis: Long
)

fun createRdbPersistence(args: Array<String>, redisConfigStore: MutableMap<String, String>) {
    // Default values
    var dir = "/tmp/redis-files"
    var dbFilename = "dump.rdb"

    // Parse command line arguments
    for (i in args.indices) {
        when (args[i]) {
            "--dir" -> if (i + 1 < args.size) dir = args[i + 1]
            "--dbfilename" -> if (i + 1 < args.size) dbFilename = args[i + 1]
        }
    }

    // Update the map with the values
    redisConfigStore["dir"] = dir
    redisConfigStore["dbFilename"] = dbFilename

    // Create directory if it doesn't exist
    val dirPath = Paths.get(dir)
    if (!Files.exists(dirPath)) {
        Files.createDirectories(dirPath)
        println("Directory created: $dir")
    } else {
        println("Directory already exists: $dir")
    }

    // Create database file
    val dbFilePath = Paths.get(dir, dbFilename)
    if (!Files.exists(dbFilePath)) {
        Files.createFile(dbFilePath)
        println("Database file created: $dbFilePath")
    } else {
        println("Database file already exists: $dbFilePath")
    }
}
