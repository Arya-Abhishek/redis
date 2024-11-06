import command.Command
import command.CommandsHandler
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import kotlin.concurrent.thread

class Server(
    private val port: Int,
    private val commandHandler: CommandsHandler
) {
    fun start() {
        val serverSocket = ServerSocket(port).apply { reuseAddress = true }
        println("Server started on port $port")
        while (true) {
            val clientSocket = serverSocket.accept()
            println("Accepted new connection")

            thread {
                clientSocket.use { socket ->
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    val outputStream = socket.getOutputStream()

                    while (true) {
                        val cmds = Command(reader)
                        if (!cmds.isValid()) break // End of current connection from same client
                        commandHandler.handleCommand(cmds, writer, outputStream)
                    }
                }
            }
        }
    }
}
