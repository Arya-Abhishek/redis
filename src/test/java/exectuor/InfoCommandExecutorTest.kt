package exectuor

import Cache.RedisCache
import command.Command
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.PrintWriter
import java.io.StringReader
import java.io.StringWriter

class InfoCommandExecutorTest {

    @Test
    fun `test replication INFO from config for master`() {
        val cmd = Command(BufferedReader(StringReader("*1\r\n$4\r\nINFO\r\n")))
        val stringWriter = StringWriter()
        val writer = PrintWriter(stringWriter)
        val redisCache = RedisCache()

        val configMap = mutableMapOf("role" to "master")
        val infoCommandExecutor = InfoCommandExecutor(configMap)
        infoCommandExecutor.execute(cmd, writer, redisCache)

        writer.flush()
        val output = stringWriter.toString()
        println(output)
    }

    @Test
    fun `test replication INFO from config for slave`() {
        val cmd = Command(BufferedReader(StringReader("*1\r\n$4\r\nINFO\r\n")))
        val stringWriter = StringWriter()
        val writer = PrintWriter(stringWriter)
        val redisCache = RedisCache()

        val configMap = mutableMapOf("role" to "slave")
        val infoCommandExecutor = InfoCommandExecutor(configMap)
        infoCommandExecutor.execute(cmd, writer, redisCache)

        writer.flush()
        val output = stringWriter.toString()
        println(output)
    }
}
