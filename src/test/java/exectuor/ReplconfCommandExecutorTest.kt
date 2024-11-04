package exectuor

import cache.RedisCache
import command.Command
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.PrintWriter
import java.io.StringReader
import java.io.StringWriter

class ReplconfCommandExecutorTest {

    @Test
    fun `test REPLCONF listening-port command`() {
        val command = Command(BufferedReader(StringReader("*3\r\n\$8\r\nREPLCONF\r\n\$14\r\nlistening-port\r\n\$4\r\n6380\r\n")))
        val stringWriter = StringWriter()
        val writer = PrintWriter(stringWriter)
        val redisCache = RedisCache()

        val replconfCommandExecutor = ReplconfCommandExecutor()
        replconfCommandExecutor.execute(command, writer, redisCache)

        writer.flush()
        val response = stringWriter.toString()
        assertEquals("+OK\r\n", response)
    }

    @Test
    fun `test REPLCONF capa psync2 command`() {
        val command = Command(BufferedReader(StringReader("*3\r\n\$8\r\nREPLCONF\r\n\$4\r\ncapa\r\n\$6\r\npsync2\r\n")))
        val stringWriter = StringWriter()
        val writer = PrintWriter(stringWriter)
        val redisCache = RedisCache()

        val replconfCommandExecutor = ReplconfCommandExecutor()
        replconfCommandExecutor.execute(command, writer, redisCache)

        writer.flush()
        val response = stringWriter.toString()
        assertEquals("+OK\r\n", response)
    }
}
