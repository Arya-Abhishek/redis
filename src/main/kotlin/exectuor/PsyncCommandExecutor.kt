package exectuor

import cache.RedisCache
import command.Command
import config.ReplicationConfig
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PrintWriter

class PsyncCommandExecutor(
    private val replicationConfig: ReplicationConfig,
    private val slavesOutputStream: MutableList<OutputStream>
): CommandExecutor {
    override fun execute(cmd: Command, writer: PrintWriter, redisCache: RedisCache, outputStream: OutputStream) {
        writer.write("+FULLRESYNC ${replicationConfig.getMasterReplid()} 0\r\n")
        writer.flush()

        // Send RDB file to the slave
        val rdbFile = File("dummy.rdb")
        FileOutputStream(rdbFile).use { fos ->
            fos.write(byteArrayOf(
                0x52, 0x45, 0x44, 0x49, 0x53, 0x30, 0x30, 0x31, 0x31, 0xfa.toByte(), 0x09, 0x72, 0x65, 0x64, 0x69, 0x73,
                0x2d, 0x76, 0x65, 0x72, 0x05, 0x37, 0x2e, 0x32, 0x2e, 0x30, 0xfa.toByte(), 0x0a, 0x72, 0x65, 0x64, 0x69,
                0x73, 0x2d, 0x62, 0x69, 0x74, 0x73, 0xc0.toByte(), 0x40, 0xfa.toByte(), 0x05, 0x63, 0x74, 0x69, 0x6d, 0x65,
                0xc2.toByte(), 0x6d, 0x08, 0xbc.toByte(), 0x65, 0xfa.toByte(), 0x08, 0x75, 0x73, 0x65, 0x64, 0x2d, 0x6d, 0x65,
                0x6d, 0xc2.toByte(), 0xb0.toByte(), 0xc4.toByte(), 0x10, 0x00, 0xfa.toByte(), 0x08, 0x61, 0x6f, 0x66, 0x2d, 0x62, 0x61,
                0x73, 0x65, 0xc0.toByte(), 0x00, 0xff.toByte(), 0xf0.toByte(), 0x6e, 0x3b, 0xfe.toByte(), 0xc0.toByte(), 0xff.toByte(), 0x5a, 0xa2.toByte()
            ))
        }

        // Add slave's output stream connection in the list
        slavesOutputStream.add(outputStream)

        val fileContents = rdbFile.readBytes()
        val fileContentsSize = "\$${fileContents.size}\r\n"
        outputStream.write(fileContentsSize.toByteArray(Charsets.UTF_8))
        outputStream.write(fileContents)
        outputStream.flush()
    }

    override fun execute(cmd: Command, writer: PrintWriter, redisCache: RedisCache) {
        throw NotImplementedError("This method is not supported")
    }
}
