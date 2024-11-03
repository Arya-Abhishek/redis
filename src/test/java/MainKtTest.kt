import command.CommandsHandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

@Disabled
class MainKtTest{

    private var serverThread: Thread? = null

    @AfterEach
    fun tearDown() {
        serverThread?.interrupt()
        serverThread = null
    }

    @Test
    fun `test PING GET SET command`() {
        serverThread = Thread {
            main(arrayOf())
        }
        serverThread?.start()

        Thread.sleep(1000)

        val socket = Socket("localhost", 6379)
        val writer = OutputStreamWriter(socket.getOutputStream())
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

        writer.write("*1\r\n$4\r\nPING\r\n")
        writer.flush()

        assertEquals("+PONG", reader.readLine())

        writer.write("*2\r\n$3\r\nGET\r\n$3\r\nkey\r\n")
        writer.flush()

        assertEquals("$-1", reader.readLine())

        writer.write("*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n")
        writer.flush()

        assertEquals("+OK", reader.readLine())

        writer.write("*2\r\n$3\r\nGET\r\n$3\r\nkey\r\n")
        writer.flush()

        assertEquals("$5", reader.readLine())
        assertEquals("value", reader.readLine())

        socket.close()
    }

    @Test
    fun `test GET command when key doesn't exist`() {
        serverThread = Thread {
            main(arrayOf())
        }
        serverThread?.start()

        Thread.sleep(1000)

        val socket = Socket("localhost", 6379)
        val writer = OutputStreamWriter(socket.getOutputStream())
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

        writer.write("*2\r\n$3\r\nGET\r\n$3\r\nkey\r\n")
        writer.flush()

        assertEquals("$-1", reader.readLine())

        socket.close()
    }

    @Test
    fun `test GET command when key exists`() {
        serverThread = Thread {
            main(arrayOf())
        }
        serverThread?.start()

        Thread.sleep(1000)

        val socket = Socket("localhost", 6379)
        val writer = OutputStreamWriter(socket.getOutputStream())
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

        writer.write("*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n")
        writer.flush()

        assertEquals("+OK", reader.readLine())

        writer.write("*2\r\n$3\r\nGET\r\n$3\r\nkey\r\n")
        writer.flush()

        assertEquals("$5", reader.readLine())
        assertEquals("value", reader.readLine())

        socket.close()
    }

    @Test
    fun `test KEYS command`() {
        serverThread = Thread {
            main(arrayOf())
        }
        serverThread?.start()

        Thread.sleep(1000)

        val socket = Socket("localhost", 6379)
        val writer = OutputStreamWriter(socket.getOutputStream())
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

        writer.write("*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n")
        writer.flush()

        assertEquals("+OK", reader.readLine())

        writer.write("*2\r\n$4\r\nKEYS\r\n$1\r\n*\r\n")
        writer.flush()

        assertEquals("*1", reader.readLine())
        assertEquals("$3", reader.readLine())
        assertEquals("key", reader.readLine())

        socket.close()
    }
}
