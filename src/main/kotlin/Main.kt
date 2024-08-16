import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket

fun main(args: Array<String>) {

     val serverSocket = ServerSocket(6379)

     serverSocket.reuseAddress = true

     val clientSocket = serverSocket.accept()

     val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
     val writer = OutputStreamWriter(clientSocket.getOutputStream())

     val command = reader.readLine()
     println("Received command: $command")

     if (command == "PING") {
          writer.write("+PONG\r\n")
          writer.flush()
     }

     println("accepted new connection")

     clientSocket.close()
}
