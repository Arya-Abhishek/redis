package exectuor

import cache.RedisCache
import command.Command
import config.ReplicationConfig
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter

class PsyncCommandExecutorTest {

    @Test
    fun testExecute() {
        // Arrange
        val replicationConfig: ReplicationConfig = mockk()
        every { replicationConfig.getMasterReplid() } returns "test-replid"

        val command: Command = mockk()
        val redisCache: RedisCache = mockk()

        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)

        val executor = PsyncCommandExecutor(replicationConfig)

        executor.execute(command, printWriter, redisCache)

        val expectedResponse = "+FULLRESYNC test-replid 0\r\n"
        assertEquals(expectedResponse, stringWriter.toString())
    }
}
