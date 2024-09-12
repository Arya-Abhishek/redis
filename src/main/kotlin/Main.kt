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
                val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                val writer = OutputStreamWriter(clientSocket.getOutputStream())

                try {
                    val commandList = parseInput(reader)
                    val commandType = commandList[0]?.trim()?.uppercase()
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
                            // ECHO will be at index 0
                            for (i in 1..<commandList.size) {
                                if (commandList[i] in listOf("", "\n", "\r\n")) continue
                                resp += "$${commandList[i].length}\r\n${commandList[i]}\r\n"
                            }
                        }
                        else -> {
                            resp = "-ERR unknown command '$commandType'\r\n"
                        }
                    }

                    writer.write(resp)
                    writer.flush()
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
    val numberOfCommands = reader.readLine() // will give, *2, *3, etc
    println("Received number of commands: $numberOfCommands")
    val numberOfCommandsInt = numberOfCommands?.substring(1)?.toInt() ?: 0
    println("Number of commands Int: $numberOfCommandsInt")

    for (i in 0 until numberOfCommandsInt) {
        val temp = reader.readLine() // will give, $3, $4, etc
        println("temp: $temp")
        val command = reader.readLine()    // will give, SET, GET, etc
        println("command: $command")
        commandList.add(command)
    }

    return commandList
}
