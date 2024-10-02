import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket

fun main(args: Array<String>) {
     val serverSocket = ServerSocket(6379)
     serverSocket.reuseAddress = true
     println("Server is running on port 6379")

     while (true) {
          val clientSocket = serverSocket.accept()

            Thread {
                val redisDataStore = mutableMapOf<String, String>()
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
                                val key = commandList[1]
                                val value = commandList[2]
                                redisDataStore[key] = value // set the value in redis
                                resp = "+OK\r\n"
                            }

                            "GET" -> {
                                val key = commandList[1]
                                val value = redisDataStore[key] ?: ""
                                resp = if (value.isNotEmpty()){ "\$${value.length}\r\n${value}\r\n" } else "\$-1\r\n"
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
