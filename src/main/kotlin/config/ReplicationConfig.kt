package config

private data class Config(
    var master_port: Int ?= 6379,
    var master_host: String? = "localhost",
    var role: String? = "master",
    var master_replid: String = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb",
    var master_repl_offset: Long = 0
)

class ReplicationConfig {
    private val config = Config()

    fun isMaster(): Boolean {
        return config.role == "master"
    }

    fun getMasterPort(): Int? {
        return config.master_port
    }

    fun getMasterHost(): String? {
        return config.master_host
    }

    fun getRole(): String? {
        return config.role
    }

    fun getMasterReplid(): String {
        return config.master_replid
    }

    fun getMasterReplOffset(): Long {
        return config.master_repl_offset
    }

    fun setMasterPort(master_port: Int) {
        config.master_port = master_port
    }

    fun setMasterHost(master_host: String) {
        config.master_host = master_host
    }

    fun setRole(role: String?) {
        config.role = role
    }

    fun setMasterReplid(master_replid: String) {
        config.master_replid = master_replid
    }

    fun setMasterReplOffset(master_repl_offset: Long) {
        config.master_repl_offset = master_repl_offset
    }
}
