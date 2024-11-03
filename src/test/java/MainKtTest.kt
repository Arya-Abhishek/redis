import command.CommandsHandler
import config.RedisConfig
import database.RedisDatabase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PushbackInputStream
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

    @Test
    fun `test KEYS command with multiple keys in RDB file`() {
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

        writer.write("*3\r\n$3\r\nSET\r\n$3\r\nfoo\r\n$5\r\nbarzu\r\n")
        writer.flush()

        assertEquals("+OK", reader.readLine())

        writer.write("*2\r\n$4\r\nKEYS\r\n$1\r\n*\r\n")
        writer.flush()

        assertEquals("*2", reader.readLine())
        assertEquals("$3", reader.readLine())
        assertEquals("foo", reader.readLine())
        assertEquals("$3", reader.readLine())
        assertEquals("key", reader.readLine())

        socket.close()
    }

    @Test
    fun `test peekByte with FileInputStream`() {
        // Create a temporary file with some test data
        val tempFile = File.createTempFile("test", "dat")
        tempFile.writeBytes(byteArrayOf(0x01, 0x02, 0x03, 0x04))

        val tempDir = tempFile.parent
        val tempFilename = tempFile.name

        // Create a FileInputStream for the temporary file
        val inputStream = FileInputStream(tempFile)
        val pushbackInputStream = PushbackInputStream(inputStream)

        // Create an instance of RedisDatabase
        val redisDatabase = RedisDatabase(RedisConfig(
            mutableMapOf(
                "dir" to tempDir,
                "dbfilename" to tempFilename
            )
        ))

        // Call the peekByte function
        val peekedByte = redisDatabase.peekByte(pushbackInputStream)

        // Read the byte again from the input stream
        val readByte = pushbackInputStream.read()

        // Check if the peeked byte and the read byte are the same
        assertEquals(peekedByte, readByte, "The peeked byte should be the same as the read byte")

        // Clean up the temporary file
        tempFile.delete()
    }

    @Test
    fun `test RDB file and GET command for single key value without any expiry`() {
        // Step 1: Create the RDB file
        val rdbFilePath = "/tmp/rdbfiles3974263201/orange.rdb"
        val rdbFile = File(rdbFilePath)
        rdbFile.parentFile.mkdirs()
        FileOutputStream(rdbFile).use { fos ->
            fos.write(byteArrayOf(
                0x52, 0x45, 0x44, 0x49, 0x53, 0x30, 0x30, 0x31, 0x31, 0xfa.toByte(), 0x09, 0x72, 0x65, 0x64, 0x69, 0x73,
                0x2d, 0x76, 0x65, 0x72, 0x05, 0x37, 0x2e, 0x32, 0x2e, 0x30, 0xfa.toByte(), 0x0a, 0x72, 0x65, 0x64, 0x69,
                0x73, 0x2d, 0x62, 0x69, 0x74, 0x73, 0xc0.toByte(), 0x40, 0xfe.toByte(), 0x00, 0xfb.toByte(), 0x01, 0x00, 0x00, 0x09, 0x70,
                0x69, 0x6e, 0x65, 0x61, 0x70, 0x70, 0x6c, 0x65, 0x0a, 0x73, 0x74, 0x72, 0x61, 0x77, 0x62, 0x65,
                0x72, 0x72, 0x79, 0xff.toByte(), 0xec.toByte(), 0xe1.toByte(), 0x7e, 0x8f.toByte(), 0xe0.toByte(), 0x32, 0x28, 0x22, 0x0a
            ))
        }

        // Step 2: Run the Redis server
        val serverThread = Thread {
            main(arrayOf("--dir", "/tmp/rdbfiles3974263201", "--dbfilename", "orange.rdb"))
        }
        serverThread.start()

        // Wait for the server to start
        Thread.sleep(1000)

        // Step 3: Execute the GET command
        val socket = Socket("localhost", 6379)
        val writer = OutputStreamWriter(socket.getOutputStream())
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

        writer.write("*2\r\n\$3\r\nGET\r\n\$9\r\npineapple\r\n")
        writer.flush()

        val response = reader.readLine()
        assertEquals("\$10", response) // Length of "strawberry"
        assertEquals("strawberry", reader.readLine())

        socket.close()
        serverThread.interrupt()
    }

//    @Test
//    fun `test RDB file and GET command for single key value without any expiry`() {
//        // Step 1: Create the RDB file
//        val rdbFilePath = "/tmp/rdbfiles3974263201/orange.rdb"
//        val rdbFile = File(rdbFilePath)
//        rdbFile.parentFile.mkdirs()
//        FileOutputStream(rdbFile).use { fos ->
//            fos.write(byteArrayOf(
//                0x52, 0x45, 0x44, 0x49, 0x53, 0x30, 0x30, 0x31, 0x31, 0xfa.toByte(), 0x09, 0x72, 0x65, 0x64, 0x69, 0x73,
//                0x2d, 0x76, 0x65, 0x72, 0x05, 0x37, 0x2e, 0x32, 0x2e, 0x30, 0xfa.toByte(), 0x0a, 0x72, 0x65, 0x64, 0x69,
//                0x73, 0x2d, 0x62, 0x69, 0x74, 0x73, 0xc0.toByte(), 0x40, 0xfe.toByte(), 0x00, 0xfb.toByte(), 0x01, 0x00, 0x00, 0x09, 0x70,
//                0x69, 0x6e, 0x65, 0x61, 0x70, 0x70, 0x6c, 0x65, 0x0a, 0x73, 0x74, 0x72, 0x61, 0x77, 0x62, 0x65,
//                0x72, 0x72, 0x79, 0xff.toByte(), 0xec.toByte(), 0xe1.toByte(), 0x7e, 0x8f.toByte(), 0xe0.toByte(), 0x32, 0x28, 0x22, 0x0a
//            ))
//        }
//
//        // Step 2: Run the Redis server
//        val serverThread = Thread {
//            main(arrayOf("--dir", "/tmp/rdbfiles3974263201", "--dbfilename", "orange.rdb"))
//        }
//        serverThread.start()
//
//        // Wait for the server to start
//        Thread.sleep(1000)
//
//        // Step 3: Execute the GET command
//        val socket = Socket("localhost", 6379)
//        val writer = OutputStreamWriter(socket.getOutputStream())
//        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
//
//        writer.write("*2\r\n\$3\r\nGET\r\n\$9\r\npineapple\r\n")
//        writer.flush()
//
//        val response = reader.readLine()
//        assertEquals("\$10", response) // Length of "strawberry"
//        assertEquals("strawberry", reader.readLine())
//
//        socket.close()
//        serverThread.interrupt()
//    }
}
