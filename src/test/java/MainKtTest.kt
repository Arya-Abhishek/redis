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
        // Read the first byte, but pushbackInputStream still have it's own pointer since test passed
        inputStream.read()

        val readByte = pushbackInputStream.read()

        // Check if the peeked byte and the read byte are the same
        assertEquals(peekedByte, readByte, "The peeked byte should be the same as the read byte")

        val ThirdByte = inputStream.read()
        val fourthPeekByte = pushbackInputStream.read()

        assertEquals(ThirdByte, 3, "The third byte should be 0x03")
        assertEquals(fourthPeekByte, 4, "The fourth byte should be 0x04")

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

    @Test
    fun `test RDB file and GET command for multiple key value without any expiry`() {
        // Step 1: Create the RDB file
        val rdbFilePath = "/tmp/rdbfiles3974263201/orange.rdb"
        val rdbFile = File(rdbFilePath)
        rdbFile.parentFile.mkdirs()

        /*
        * RDB file content -> having 5 key-value pairs
        * JW4] Idx  | Hex                                             | ASCII
[tester::#JW4] -----+-------------------------------------------------+-----------------
[tester::#JW4] 0000 | 52 45 44 49 53 30 30 31 31 fa 09 72 65 64 69 73 | REDIS0011..redis
[tester::#JW4] 0010 | 2d 76 65 72 05 37 2e 32 2e 30 fa 0a 72 65 64 69 | -ver.7.2.0..redi
[tester::#JW4] 0020 | 73 2d 62 69 74 73 c0 40 fe 00 fb 05 00 00 09 72 | s-bits.@.......r
[tester::#JW4] 0030 | 61 73 70 62 65 72 72 79 0a 73 74 72 61 77 62 65 | aspberry.strawbe
[tester::#JW4] 0040 | 72 72 79 00 06 62 61 6e 61 6e 61 05 61 70 70 6c | rry..banana.appl
[tester::#JW4] 0050 | 65 00 0a 73 74 72 61 77 62 65 72 72 79 06 6f 72 | e..strawberry.or
[tester::#JW4] 0060 | 61 6e 67 65 00 05 61 70 70 6c 65 09 70 69 6e 65 | ange..apple.pine
[tester::#JW4] 0070 | 61 70 70 6c 65 00 05 67 72 61 70 65 05 6d 61 6e | apple..grape.man
[tester::#JW4] 0080 | 67 6f ff 04 3c 14 16 58 10 33 4e 0a             | go..<..X.3N.
        *
        * */
        FileOutputStream(rdbFile).use { fos ->
            fos.write(byteArrayOf(
                0x52, 0x45, 0x44, 0x49, 0x53, 0x30, 0x30, 0x31, 0x31, 0xfa.toByte(), 0x09, 0x72, 0x65, 0x64, 0x69, 0x73,
                0x2d, 0x76, 0x65, 0x72, 0x05, 0x37, 0x2e, 0x32, 0x2e, 0x30, 0xfa.toByte(), 0x0a, 0x72, 0x65, 0x64, 0x69,
                0x73, 0x2d, 0x62, 0x69, 0x74, 0x73, 0xc0.toByte(), 0x40, 0xfe.toByte(), 0x00, 0xfb.toByte(), 0x05, 0x00, 0x00, 0x09, 0x72,
                0x61, 0x73, 0x70, 0x62, 0x65, 0x72, 0x72, 0x79, 0x0a, 0x73, 0x74, 0x72, 0x61, 0x77, 0x62, 0x65, 0x72,
                0x72, 0x79, 0x00, 0x06, 0x62, 0x61, 0x6e, 0x61, 0x6e, 0x61, 0x05, 0x61, 0x70, 0x70, 0x6c, 0x65, 0x00,
                0x0a, 0x73, 0x74, 0x72, 0x61, 0x77, 0x62, 0x65, 0x72, 0x72, 0x79, 0x06, 0x6f, 0x72, 0x61, 0x6e, 0x67,
                0x65, 0x00, 0x05, 0x61, 0x70, 0x70, 0x6c, 0x65, 0x09, 0x70, 0x69, 0x6e, 0x65, 0x61, 0x70, 0x70, 0x6c,
                0x65, 0x00, 0x05, 0x67, 0x72, 0x61, 0x70, 0x65, 0x05, 0x6d, 0x61, 0x6e, 0x67, 0x6f, 0xff.toByte(), 0x04,
                0x3c, 0x14, 0x16, 0x58, 0x10, 0x33, 0x4e, 0x0a
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

        writer.write("*2\r\n\$3\r\nGET\r\n\$9\r\nraspberry\r\n")
        writer.flush()

        writer.write("*2\r\n\$3\r\nGET\r\n\$5\r\ngrape\r\n")
        writer.flush()

        val response = reader.readLine()
        assertEquals("\$10", response) // Length of "strawberry"
        assertEquals("strawberry", reader.readLine())

        val response2 = reader.readLine()
        assertEquals("\$5", response2) // Length of "banana"
        assertEquals("mango", reader.readLine())

        socket.close()
        serverThread.interrupt()
    }

    @Test
    fun `test RDB file and GET command for multiple key value with expiry`() {
        // Step 1: Create the RDB file
        val rdbFilePath = "/tmp/rdbfiles3974263201/orange.rdb"
        val rdbFile = File(rdbFilePath)
        rdbFile.parentFile.mkdirs()

        /*
        * [tester::#SM4] Created RDB file with 4 key-value pairs: {"pineapple": "orange", "raspberry": "banana", "blueberry": "pineapple", "orange": "mango"}
        [tester::#SM4] Hexdump of RDB file contents:
        [tester::#SM4] Idx  | Hex                                             | ASCII
        [tester::#SM4] -----+-------------------------------------------------+-----------------
        [tester::#SM4] 0000 | 52 45 44 49 53 30 30 31 31 fa 0a 72 65 64 69 73 | REDIS0011..redis
        [tester::#SM4] 0010 | 2d 62 69 74 73 c0 40 fa 09 72 65 64 69 73 2d 76 | -bits.@..redis-v
        [tester::#SM4] 0020 | 65 72 05 37 2e 32 2e 30 fe 00 fb 04 04 fc 00 0c | er.7.2.0........
        [tester::#SM4] 0030 | 28 8a c7 01 00 00 00 09 70 69 6e 65 61 70 70 6c | (.......pineappl
        [tester::#SM4] 0040 | 65 06 6f 72 61 6e 67 65 fc 00 0c 28 8a c7 01 00 | e.orange...(....
        [tester::#SM4] 0050 | 00 00 09 72 61 73 70 62 65 72 72 79 06 62 61 6e | ...raspberry.ban
        [tester::#SM4] 0060 | 61 6e 61 fc 00 9c ef 12 7e 01 00 00 00 09 62 6c | ana.....~.....bl
        [tester::#SM4] 0070 | 75 65 62 65 72 72 79 09 70 69 6e 65 61 70 70 6c | ueberry.pineappl
        [tester::#SM4] 0080 | 65 fc 00 0c 28 8a c7 01 00 00 00 06 6f 72 61 6e | e...(.......oran
        [tester::#SM4] 0090 | 67 65 05 6d 61 6e 67 6f ff 10 8b 91 6f 2d 93 27 | ge.mango....o-.'
        [tester::#SM4] 00a0 | 7d 0a
        *
        * */
        FileOutputStream(rdbFile).use { fos ->
            fos.write(byteArrayOf(
                0x52, 0x45, 0x44, 0x49, 0x53, 0x30, 0x30, 0x31, 0x31, 0xfa.toByte(), 0x0a, 0x72, 0x65, 0x64, 0x69, 0x73,
                0x2d, 0x62, 0x69, 0x74, 0x73, 0xc0.toByte(), 0x40, 0xfa.toByte(), 0x09, 0x72, 0x65, 0x64, 0x69, 0x73, 0x2d, 0x76,
                0x65, 0x72, 0x05, 0x37, 0x2e, 0x32, 0x2e, 0x30, 0xfe.toByte(), 0x00, 0xfb.toByte(), 0x04, 0x04, 0xfc.toByte(), 0x00, 0x0c,
                0x28, 0x8a.toByte(), 0xc7.toByte(), 0x01, 0x00, 0x00, 0x00, 0x09, 0x70, 0x69, 0x6e, 0x65, 0x61, 0x70, 0x70, 0x6c,
                0x65, 0x06, 0x6f, 0x72, 0x61, 0x6e, 0x67, 0x65, 0xfc.toByte(), 0x00, 0x0c, 0x28, 0x8a.toByte(), 0xc7.toByte(), 0x01, 0x00,
                0x00, 0x00, 0x09, 0x72, 0x61, 0x73, 0x70, 0x62, 0x65, 0x72, 0x72, 0x79, 0x06, 0x62, 0x61, 0x6e,
                0x61, 0x6e, 0x61, 0xfc.toByte(), 0x00, 0x9c.toByte(), 0xef.toByte(), 0x12, 0x7e, 0x01, 0x00, 0x00, 0x00, 0x09, 0x62, 0x6c,
                0x75, 0x65, 0x62, 0x65, 0x72, 0x72, 0x79, 0x09, 0x70, 0x69, 0x6e, 0x65, 0x61, 0x70, 0x70, 0x6c,
                0x65, 0xfc.toByte(), 0x00, 0x0c, 0x28, 0x8a.toByte(), 0xc7.toByte(), 0x01, 0x00, 0x00, 0x00, 0x06, 0x6f, 0x72, 0x61, 0x6e,
                0x67, 0x65, 0x05, 0x6d, 0x61, 0x6e, 0x67, 0x6f, 0xff.toByte(), 0x10, 0x8b.toByte(), 0x91.toByte(), 0x6f, 0x2d, 0x93.toByte(), 0x27,
                0x7d, 0x0a
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

        writer.write("*2\r\n\$3\r\nGET\r\n\$6\r\norange\r\n")
        writer.flush()

        val response = reader.readLine()
        assertEquals("\$6", response) // Length of "strawberry"
        assertEquals("orange", reader.readLine())

        val response2 = reader.readLine()
        assertEquals("\$5", response2) // Length of "mango"
        assertEquals("mango", reader.readLine())

        socket.close()
        serverThread.interrupt()
    }

    @Test
    fun `should read 64 bits for expiry time correctly`() {
        val byteArray = byteArrayOf(0x00, 0x9c.toByte(), 0xef.toByte(), 0x12, 0x7e, 0x01, 0x00, 0x00)

        // Create a temporary file with the byte array
        val tempFile = File.createTempFile("test", "dat")
        tempFile.writeBytes(byteArray)

        // Create a FileInputStream for the temporary file
        val inputStream = FileInputStream(tempFile)

        // Call the read64bitInteger function with the FileInputStream
        val expiry = RedisDatabase.read64bitInteger(inputStream)
        val currentTime = System.currentTimeMillis()

        // Print the result
        println("expire: $expiry currentTime: $currentTime")

        // Clean up the temporary file
        tempFile.delete()
    }
}
