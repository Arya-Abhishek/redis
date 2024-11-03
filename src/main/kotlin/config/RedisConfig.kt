package config

class RedisConfig(val configMap: MutableMap<String, String>) {

    fun dir(): String? {
        return configMap["dir"]
    }

    fun dbfilename(): String? {
        return configMap["dbfilename"]
    }

    fun dbPath(): String {
        return "${dir()}/${dbfilename()}"
    }
}
