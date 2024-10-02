import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

class MainKtTest{

    private var serverThread: Thread? = null

    @AfterEach
    fun tearDown() {
        serverThread?.interrupt()
        serverThread = null
    }

    @Test
    fun testPing() {
        val serverThread = Thread {
            main(arrayOf())
        }
        serverThread.start()

        Thread.sleep(1000)

        val clientSocket = Socket("localhost", 6379)
        val writer = OutputStreamWriter(clientSocket.getOutputStream())
        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))

        // Test PING command
        writer.write("*1\r\n\$4\r\nPING\r\n")
        writer.flush()
        val response = reader.readLine()
        assertEquals("+PONG", response)

        clientSocket.close()
        serverThread.interrupt()
    }

    @Test
    fun testEcho() {
        val serverThread = Thread {
            main(arrayOf())
        }
        serverThread.start()

        Thread.sleep(1000)

        val clientSocket = Socket("localhost", 6379)
        val writer = OutputStreamWriter(clientSocket.getOutputStream())
        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))

        // Test ECHO command
        writer.write("*2\r\n\$4\r\nECHO\r\n\$5\r\nhello\r\n")
        writer.flush()
        var response = reader.readLine()
        assertEquals("\$5", response)
        response = reader.readLine()
        assertEquals("hello", response)

        clientSocket.close()
        serverThread.interrupt()
    }

    @Test
    fun testSetAndGetWhenEntryExists() {
        val serverThread = Thread {
            main(arrayOf())
        }
        serverThread.start()

        val clientSocket = Socket("localhost", 6379)
        val writer = OutputStreamWriter(clientSocket.getOutputStream())
        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))

        // Test SET command
        writer.write("*3\r\n\$3\r\nSET\r\n\$3\r\nkey\r\n\$5\r\nvalue\r\n")
        writer.flush()
        var response = reader.readLine()
        assertEquals("+OK", response)

        // Test GET command
        writer.write("*2\r\n\$3\r\nGET\r\n\$3\r\nkey\r\n")
        writer.flush()
        response = reader.readLine()
        assertEquals("\$5", response)
        response = reader.readLine()
        assertEquals("value", response)

        clientSocket.close()
        serverThread.interrupt()
    }

    @Disabled
    @Test
    fun testSetAndGetWhenEntryDoesNotExist() {
        val serverThread = Thread {
            main(arrayOf())
        }
        serverThread.start()

        val clientSocket = Socket("localhost", 6379)
        val writer = OutputStreamWriter(clientSocket.getOutputStream())
        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))

        // Test GET command
        writer.write("*2\r\n\$3\r\nGET\r\n\$3\r\nkey\r\n")
        writer.flush()
        var response = reader.readLine()
        assertEquals("\$-1", response)

        clientSocket.close()
        serverThread.interrupt()
    }

    @Test
    fun testSetAndGetWhenEntryExistWithMultipleCommandsOnSameConnection() {
        val serverThread = Thread {
            main(arrayOf())
        }
        serverThread.start()

        val clientSocket = Socket("localhost", 6379)
        val writer = OutputStreamWriter(clientSocket.getOutputStream())
        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))

        // Test SET command
        writer.write("*3\r\n\$3\r\nSET\r\n\$3\r\nkey\r\n\$5\r\nvalue\r\n")
        writer.flush()
        var response = reader.readLine()
        assertEquals("+OK", response)

        // Test GET command
        writer.write("*2\r\n\$3\r\nGET\r\n\$3\r\nkey\r\n")
        writer.flush()
        response = reader.readLine()
        assertEquals("\$5", response)
        response = reader.readLine()
        assertEquals("value", response)

        // Test SET command
        writer.write("*3\r\n\$3\r\nSET\r\n\$3\r\nkey2\r\n\$5\r\nvalue2\r\n")
        writer.flush()
        response = reader.readLine()
        assertEquals("+OK", response)

        // Test GET command
        writer.write("*2\r\n\$3\r\nGET\r\n\$4\r\nkey2\r\n")
        writer.flush()
        response = reader.readLine()
        assertEquals("\$6", response)
        response = reader.readLine()
        assertEquals("value2", response)

        clientSocket.close()
        serverThread.interrupt()
    }

    @Disabled
    @Test
    fun testTwoClientsWithSeparateDataStores() {
        val serverThread = Thread {
            main(arrayOf())
        }
        serverThread.start()

        Thread.sleep(1000)

        // Client 1
        val clientSocket1 = Socket("localhost", 6379)
        val writer1 = OutputStreamWriter(clientSocket1.getOutputStream())
        val reader1 = BufferedReader(InputStreamReader(clientSocket1.getInputStream()))

        // Client 2
        val clientSocket2 = Socket("localhost", 6379)
        val writer2 = OutputStreamWriter(clientSocket2.getOutputStream())
        val reader2 = BufferedReader(InputStreamReader(clientSocket2.getInputStream()))

        // Client 1: SET command
        writer1.write("*3\r\n\$3\r\nSET\r\n\$3\r\nkey1\r\n\$5\r\nvalue1\r\n")
        writer1.flush()
        var response1 = reader1.readLine()
        assertEquals("+OK", response1)

        // Client 2: SET command
        writer2.write("*3\r\n\$3\r\nSET\r\n\$3\r\nkey2\r\n\$5\r\nvalue2\r\n")
        writer2.flush()
        var response2 = reader2.readLine()
        assertEquals("+OK", response2)

        // Client 1: GET command
        writer1.write("*2\r\n\$3\r\nGET\r\n\$3\r\nkey1\r\n")
        writer1.flush()
        response1 = reader1.readLine()
        assertEquals("\$6", response1)
        response1 = reader1.readLine()
        assertEquals("value1", response1)

        // Client 2: GET command
        writer2.write("*2\r\n\$3\r\nGET\r\n\$3\r\nkey2\r\n")
        writer2.flush()
        response2 = reader2.readLine()
        assertEquals("\$6", response2)
        response2 = reader2.readLine()
        assertEquals("value2", response2)

        // Client 1: GET command for key2 (should not exist)
        writer1.write("*2\r\n\$3\r\nGET\r\n\$3\r\nkey2\r\n")
        writer1.flush()
        response1 = reader1.readLine()
        assertEquals("\$-1", response1)

        // Client 2: GET command for key1 (should not exist)
        writer2.write("*2\r\n\$3\r\nGET\r\n\$3\r\nkey1\r\n")
        writer2.flush()
        response2 = reader2.readLine()
        assertEquals("\$-1", response2)

        clientSocket1.close()
        clientSocket2.close()
        serverThread.interrupt()
    }

    @Test
    fun testSetWithExpiryAndGetWithinExpiry() {
        val serverThread = Thread {
            main(arrayOf())
        }
        serverThread.start()

        Thread.sleep(1000) // Give the server some time to start

        val clientSocket = Socket("localhost", 6379)
        val writer = OutputStreamWriter(clientSocket.getOutputStream())
        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))

        // Test SET command with PX option
        writer.write("*5\r\n\$3\r\nSET\r\n\$3\r\nfoo\r\n\$3\r\nbar\r\n\$2\r\nPX\r\n\$3\r\n1000\r\n")
        writer.flush()
        var response = reader.readLine()
        assertEquals("+OK", response)

        // Test GET command within expiry time
        writer.write("*2\r\n\$3\r\nGET\r\n\$3\r\nfoo\r\n")
        writer.flush()
        response = reader.readLine()
        assertEquals("\$3", response)
        response = reader.readLine()
        assertEquals("bar", response)

        clientSocket.close()
        serverThread.interrupt()
    }

    @Test
    fun testSetWithExpiryAndGetAfterExpiry() {
        val serverThread = Thread {
            main(arrayOf())
        }
        serverThread.start()

        Thread.sleep(1000) // Give the server some time to start

        val clientSocket = Socket("localhost", 6379)
        val writer = OutputStreamWriter(clientSocket.getOutputStream())
        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))

        // Test SET command with PX option
        writer.write("*5\r\n\$3\r\nSET\r\n\$3\r\nfoo\r\n\$3\r\nbar\r\n\$2\r\nPX\r\n\$3\r\n1000\r\n")
        writer.flush()
        var response = reader.readLine()
        assertEquals("+OK", response)

        // Wait for the expiry time to pass
        Thread.sleep(1500)

        // Test GET command after expiry time
        writer.write("*2\r\n\$3\r\nGET\r\n\$3\r\nfoo\r\n")
        writer.flush()
        response = reader.readLine()
        assertEquals("\$-1", response)

        clientSocket.close()
        serverThread.interrupt()
    }

    @Test
    fun testConfigGetCommands() {
        val serverThread = Thread {
            main(arrayOf("--dir", "/tmp/redis-files", "--dbfilename", "dump.rdb"))
        }
        serverThread.start()

        Thread.sleep(1000) // Give the server some time to start

        val clientSocket = Socket("localhost", 6379)
        val writer = OutputStreamWriter(clientSocket.getOutputStream())
        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))

        // Test CONFIG GET dir command
        writer.write("*3\r\n\$6\r\nCONFIG\r\n\$3\r\nGET\r\n\$3\r\ndir\r\n")
        writer.flush()
        var response = reader.readLine()
        assertEquals("*2", response)
        response = reader.readLine()
        assertEquals("\$3", response)
        response = reader.readLine()
        assertEquals("dir", response)
        response = reader.readLine()
        assertEquals("\$16", response)
        response = reader.readLine()
        assertEquals("/tmp/redis-files", response)

        // Test CONFIG GET dbfilename command
        writer.write("*3\r\n\$6\r\nCONFIG\r\n\$3\r\nGET\r\n\$10\r\ndbfilename\r\n")
        writer.flush()
        response = reader.readLine()
        assertEquals("*2", response)
        response = reader.readLine()
        assertEquals("\$10", response)
        response = reader.readLine()
        assertEquals("dbfilename", response)
        response = reader.readLine()
        assertEquals("\$8", response)
        response = reader.readLine()
        assertEquals("dump.rdb", response)

        clientSocket.close()
        serverThread.interrupt()
    }
}
