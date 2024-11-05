package exectuor

import cache.RedisCache
import command.Command
import config.ReplicationConfig
import java.io.PrintWriter

class PsyncCommandExecutor(
    private val replicationConfig: ReplicationConfig
): CommandExecutor {
    override fun execute(cmd: Command, writer: PrintWriter, redisCache: RedisCache) {
        /*
        * No subcommand, The master needs to respond with +FULLRESYNC <REPL_ID> 0\r\n ("FULLRESYNC 0" encoded as a RESP Simple String).
        * */

        writer.write("+FULLRESYNC ${replicationConfig.getMasterReplid()} 0\r\n")
        writer.flush()
    }
}
