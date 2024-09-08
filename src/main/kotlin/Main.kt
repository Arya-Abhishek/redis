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

          val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
          val writer = OutputStreamWriter(clientSocket.getOutputStream())

            Thread {
                 try {
                        var command = reader.readLine()
                        do {
                             if (command.trim().uppercase() == "PING") {
                                writer.write("+PONG\r\n")
                                writer.flush()
                             }
                             command = reader.readLine()
                        } while (command != null)
                 } catch (e: Exception) {
                        println("Error: $e")
                 } finally {
                      try {
                           clientSocket.close()
                      } catch (e: java.lang.Exception) {
                           println("Error closing socket: $e")
                      }
                 }
            }.start()
     }
}
