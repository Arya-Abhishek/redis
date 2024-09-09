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
                    val command = reader.readText()
                    val commandList = command.split("\r\n")
                    val commandType = commandList[2]?.trim()?.uppercase()
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
                            val idxOfEcho = commandList.indexOf("ECHO")
                            for (i in idxOfEcho+1..<commandList.size) {
                                if (commandList[i] in listOf("", "\n", "\r\n")) continue
                                resp += "${commandList[i]}\r\n"
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
