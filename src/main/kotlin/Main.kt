import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.CRC32

fun main(args: Array<String>) {
    // RDB Persistence
    createRdbPersistence(args)

     val serverSocket = ServerSocket(6379)
     serverSocket.reuseAddress = true
     println("Server is running on port 6379")

     while (true) {
          val clientSocket = serverSocket.accept()

            Thread {

                val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                val writer = OutputStreamWriter(clientSocket.getOutputStream())

                try {
                    while (true) {
                        val commandList = parseInput(reader)
                        if (commandList.isEmpty()) break
                        val commandType = commandList[0].trim().uppercase()
                        var resp = ""
                        println("CommandList: $commandList")
                        when (commandType) {
                            "PING" -> {
                                commandList.forEach {
                                    if (it.trim().uppercase() == "PING") {
                                        resp += "+PONG\r\n"
                                    }
                                }
                            }

                            "ECHO" -> {
                                for (i in 1..<commandList.size) {
                                    if (commandList[i] in listOf("", "\n", "\r\n")) continue
                                    resp += "$${commandList[i].length}\r\n${commandList[i]}\r\n"
                                }
                            }

                            "SET" -> {
                                val key = commandList[1] // get the key
                                val value = commandList[2] // get the value
                                if (commandList.size > 3 && commandList[3].uppercase() == "PX") {
                                    val expiryTime = commandList[4].toLong() // get the expiry time
                                    RedisDataStore.dataStore[key] = value // set the value in redis
                                    RedisKeyExpiry.keyExpiry[key] = System.currentTimeMillis() + expiryTime // set the expiry time
                                } else {
                                    RedisDataStore.dataStore[key] = value // set the value in redis
                                }
                                resp = "+OK\r\n"
                            }

                            "GET" -> {
                                val key = commandList[1]
                                val value = getValue(key)
                                resp = if (value.isNotEmpty()){ "\$${value.length}\r\n${value}\r\n" } else "\$-1\r\n"
                            }

                            "CONFIG" -> {
                                if (commandList.size > 1 && commandList[1].uppercase() == "GET") {
                                    val configKey = commandList[2]
                                    resp = "*2\r\n"
                                    when (configKey) {
                                        "dir" -> {
                                            val dir = RedisConfigStore.configStore["dir"] ?: ""
                                            resp += "\$${"dir".length}\r\ndir\r\n\$${dir.length}\r\n${dir}\r\n"
                                        }
                                        "dbfilename" -> {
                                            val dbFilename = RedisConfigStore.configStore["dbFilename"] ?: ""
                                            resp += "\$${"dbfilename".length}\r\ndbfilename\r\n\$${dbFilename.length}\r\n${dbFilename}\r\n"
                                        }
                                        else -> {
                                            resp = "-ERR unknown CONFIG parameter '$configKey'\r\n"
                                        }
                                    }
                                } else { // config set
                                    resp = "-ERR CONFIG subcommand must be one of GET\r\n"
                                }
                            }

                            "SAVE" -> saveHashTablesToRdbFile() // Implementation NOT needed

                            "KEYS" -> {
                                val listOfKeys = getAllKeysMatchingPattern(commandList[1])
                                println( "List of keys: $listOfKeys" )
                                // iterate and build the resp output for list of keys
                                resp = "*${listOfKeys.size}\r\n"
                                listOfKeys.forEach {
                                    resp += "\$${it.length}\r\n$it\r\n"
                                }
                            }

                            else -> {
                                resp = "-ERR unknown command '$commandType'\r\n"
                            }
                        }

                        writer.write(resp)
                        writer.flush()
                    }
                } catch (e: Exception) {
                    println("Error: $e")
                } finally {
                    clientSocket.close()
                }
            }.start()
     }
}

fun getAllKeysMatchingPattern(pattern: String): List<String> {
    val dbFilePath = getDbFilePath()
    val matchingKeys = mutableListOf<String>()
    val dbHeaderOpcode = 0xFE.toByte()

    // Convert the pattern to a regular expression
    val regexPattern = pattern.replace("*", ".*").toRegex()

    try {
        FileInputStream(dbFilePath).use { fis ->
            var byteRead: Int
            var inDatabaseSection = false

            while (fis.read().also { byteRead = it } != -1) {
                if (byteRead.toByte() == dbHeaderOpcode) {
                    inDatabaseSection = true
                }

                if (inDatabaseSection) {
                    // skip the database header section
                    fis.skip(4)

                    // Read the key-value pairs
                    while (fis.read().also { byteRead = it } != -1) {
                        when (byteRead.toByte()) {
                            0x00.toByte() -> { // Value type is string
                                println("Value type: ${byteRead.toString(16)}")
                                val keyLength = fis.read()
                                println("Key length: ${keyLength.toString(16)}")
                                if (keyLength == -1) break
                                val keyBytes = ByteArray(keyLength)
                                if (fis.read(keyBytes) != keyLength) break
                                val key = String(keyBytes)
                                println("key: $key")

                                // Check if the key matches the pattern
                                if (key.matches(regexPattern)) {
                                    matchingKeys.add(key)
                                }

                                // Skip the value length and value bytes
                                val valueLength = fis.read()
                                if (valueLength == -1) break
                                fis.skip(valueLength.toLong())
                            }
                            0xFC.toByte() -> { // Expiry in milliseconds
                                fis.skip(8) // Skip the 8-byte expiry timestamp
                                val keyLength = fis.read()
                                if (keyLength == -1) break
                                val keyBytes = ByteArray(keyLength)
                                if (fis.read(keyBytes) != keyLength) break
                                val key = String(keyBytes)

                                // Check if the key matches the pattern
                                if (key.matches(regexPattern)) {
                                    matchingKeys.add(key)
                                }

                                // Skip the value length and value bytes
                                val valueLength = fis.read()
                                if (valueLength == -1) break
                                fis.skip(valueLength.toLong())
                            }
                            0xFD.toByte() -> { // Expiry in seconds
                                fis.skip(4) // Skip the 4-byte expiry timestamp
                                val keyLength = fis.read()
                                if (keyLength == -1) break
                                val keyBytes = ByteArray(keyLength)
                                if (fis.read(keyBytes) != keyLength) break
                                val key = String(keyBytes)

                                // Check if the key matches the pattern
                                if (key.matches(regexPattern)) {
                                    matchingKeys.add(key)
                                }

                                // Skip the value length and value bytes
                                val valueLength = fis.read()
                                if (valueLength == -1) break
                                fis.skip(valueLength.toLong())
                            }
                        }
                    }
                }
            }
        }
    } catch (e: IOException) {
        println("Error while reading RDB file: $e")
    }

    return matchingKeys
}

fun saveHashTablesToRdbFile() {
    writeHeader()
    writeMetaData()
    writeDatabaseSection()
    writeEOFSection()
}

fun writeHeader() {
    val dbFilePath = getDbFilePath()
    val expectedHeader = "REDIS0006"
    if (!checkHeaderAlreadyExists(dbFilePath, expectedHeader)) {
        try {
            val headerBytes = expectedHeader.toByteArray()
            FileOutputStream(dbFilePath).use { fos ->
                fos.write(headerBytes)
            }
        } catch (e: Exception) {
            println("Error while writing header into RDB file: $e")
        }
    }
}

fun checkHeaderAlreadyExists(dbFilePath: String, expectedHeader: String): Boolean {
    val headerBytes = expectedHeader.toByteArray()
    val headerLength = headerBytes.size

    return try {
        FileInputStream(dbFilePath).use { fis ->
            val fileHeader = ByteArray(headerLength)
            if (fis.read(fileHeader) != headerLength) {
                return false
            }
            fileHeader.contentEquals(headerBytes)
        }
    } catch (e: Exception) {
        println("Error: $e")
        false
    }
}

fun writeMetaData() {
    val metadataOpcode = 0xFA.toByte()
    val REDIS_VER = "redis-ver"
    val REDIS_VER_VALUE = "6.0.10"
    val redisVerBytes = REDIS_VER.toByteArray()
    val redisVerValueBytes = REDIS_VER_VALUE.toByteArray()
    val bytesToWrite = ByteArray(1 + redisVerBytes.size + redisVerValueBytes.size)
    bytesToWrite[0] = metadataOpcode

    System.arraycopy(redisVerBytes, 0, bytesToWrite, 1, redisVerBytes.size)
    System.arraycopy(redisVerValueBytes, 0, bytesToWrite, 1 + redisVerBytes.size, redisVerValueBytes.size)

    val dbFilePath = getDbFilePath()
    if (!checkIfOpcodeSectionAlreadyExists(0xFA.toByte())) {
        try {
            FileOutputStream(dbFilePath, true).use { fos ->
                fos.write(bytesToWrite)
            }
        } catch (e: Exception) {
            println("Error while writing metadata into RDB file: $e")
        }
    }
}

fun checkIfOpcodeSectionAlreadyExists(Opcode: Byte): Boolean {
    val dbFilePath = getDbFilePath()
    val metadataOpcode = Opcode
    val metadataOpcodeBytes = ByteArray(1)
    metadataOpcodeBytes[0] = metadataOpcode

    return try {
        FileInputStream(dbFilePath).use { fis ->
            var byteRead: Int

            while (fis.read().also { byteRead = it } != -1) {
                if (byteRead.toByte() == metadataOpcode) {
                    return true
                }
            }
            return false
        }
    } catch (e: Exception) {
        println("Error while reading metadata section in RDB file: $e")
        false
    }
}

fun getDbFilePath(): String {
    val dir = RedisConfigStore.configStore["dir"]
    val dbFilename = RedisConfigStore.configStore["dbFilename"]
    val dbFilePath = Paths.get(dir, dbFilename).toString()
    return dbFilePath
}

fun writeDatabaseSection() {

    writeDataBaseHeaderSection()

    // Now write the key-value pairs
    writeKeyValuePairs()
}

fun writeDataBaseHeaderSection() {
    val dbFilePath = getDbFilePath()
    val dbHeaderOpcode = 0xFE.toByte()
    val redisDataStoreSize = RedisDataStore.dataStore.size.toString(16)
    val redisKeyExpirySize = RedisKeyExpiry.keyExpiry.size.toString(16)
    val dbHeaderBytes = byteArrayOf(0xFE.toByte(), 0x00.toByte(), redisDataStoreSize.toByte(), redisKeyExpirySize.toByte())

    if (!checkIfOpcodeSectionAlreadyExists(dbHeaderOpcode)) {
        try {
            FileOutputStream(dbFilePath, true).use { fos ->
                fos.write(dbHeaderBytes)
            }
        } catch (e: Exception) {
            println("Error while writing database header into RDB file: $e")
        }
    }
}

fun writeKeyValuePairs() {
    val dbFilePath = getDbFilePath()
    FileOutputStream(dbFilePath, true).use { fos ->
        RedisDataStore.dataStore.forEach { (key, value) ->
            val keyBytes = key.toByteArray()
            val valueBytes = value.toByteArray()
            val expiryTime = RedisKeyExpiry.keyExpiry[key]

            if (expiryTime == null) {
                // Case 1: No expiry time
                fos.write(byteArrayOf(0x00.toByte())) // Value type is string
                fos.write(byteArrayOf(keyBytes.size.toByte()))
                fos.write(keyBytes)
                fos.write(byteArrayOf(valueBytes.size.toByte()))
                fos.write(valueBytes)
            } else {
                val currentTime = System.currentTimeMillis()
                if (expiryTime > currentTime) {
                    val remainingTime = expiryTime - currentTime
                    if (remainingTime > 0xFFFFFFFFL) {
                        // Case 2: Expiry time in milliseconds
                        fos.write(byteArrayOf(0xFC.toByte())) // Expiry in milliseconds
                        fos.write(longToLittleEndianBytes(expiryTime))
                    } else {
                        // Case 3: Expiry time in seconds
                        fos.write(byteArrayOf(0xFD.toByte())) // Expiry in seconds
                        fos.write(intToLittleEndianBytes((expiryTime / 1000).toInt()))
                    }
                    fos.write(byteArrayOf(0x00.toByte())) // Value type is string
                    fos.write(byteArrayOf(keyBytes.size.toByte()))
                    fos.write(keyBytes)
                    fos.write(byteArrayOf(valueBytes.size.toByte()))
                    fos.write(valueBytes)
                }
            }
        }
    }
}

fun longToLittleEndianBytes(value: Long): ByteArray {
    return ByteArray(8) { i -> (value shr (i * 8)).toByte() }
}

fun intToLittleEndianBytes(value: Int): ByteArray {
    return ByteArray(4) { i -> (value shr (i * 8)).toByte() }
}

fun writeEOFSection() {
    val dbFilePath = getDbFilePath()
    val eofByte = 0xFF.toByte()

    try {
        FileOutputStream(dbFilePath, true).use { fos ->
            // Write the EOF byte
            fos.write(byteArrayOf(eofByte))

            // Calculate the CRC64 checksum
            val checksum = calculateCRC64(dbFilePath)
            fos.write(longToLittleEndianBytes(checksum))
        }
    } catch (e: Exception) {
        println("Error while writing EOF section into RDB file: $e")
    }
}

fun calculateCRC64(filePath: String): Long {
    val crc = CRC32()
    FileInputStream(filePath).use { fis ->
        val buffer = ByteArray(1024)
        var bytesRead: Int
        while (fis.read(buffer).also { bytesRead = it } != -1) {
            crc.update(buffer, 0, bytesRead)
        }
    }
    return crc.value
}

fun parseInput(reader: BufferedReader): List<String> {
    val commandList = mutableListOf<String>()
    val numberOfCommands = reader.readLine() ?: return emptyList() // will give, *2, *3, etc
    val numberOfCommandsInt = numberOfCommands.substring(1).toInt() ?: 0

    for (i in 0 until numberOfCommandsInt) {
        reader.readLine() // will give, $3, $4, etc
        val command = reader.readLine()    // will give, SET, GET, etc
        commandList.add(command)
    }

    return commandList
}

fun getValue(key: String): String {
    val redisValue = RedisDataStore.dataStore[key] ?: return ""
    val redisKeyExpiry = RedisKeyExpiry.keyExpiry
    if (redisKeyExpiry.containsKey(key) && redisKeyExpiry[key] != null) {
        if (redisKeyExpiry[key]!! < System.currentTimeMillis()) {
            RedisKeyExpiry.keyExpiry.remove(key)
            return ""
        }
    }

    return redisValue
}

data class RedisValue(
    val value: String,
    val expiryTimeMillis: Long?,
    val lastSetTimeMillis: Long
)

fun createRdbPersistence(args: Array<String>) {
    // Default values
    var dir = "/tmp/redis-files"
    var dbFilename = "dump.rdb"

    // Parse command line arguments
    for (i in args.indices) {
        when (args[i]) {
            "--dir" -> if (i + 1 < args.size) dir = args[i + 1]
            "--dbfilename" -> if (i + 1 < args.size) dbFilename = args[i + 1]
        }
    }

    // Update the map with the values
    RedisConfigStore.configStore["dir"] = dir
    RedisConfigStore.configStore["dbFilename"] = dbFilename

    // Create directory if it doesn't exist
    val dirPath = Paths.get(dir)
    if (!Files.exists(dirPath)) {
        Files.createDirectories(dirPath)
        println("Directory created: $dir")
    } else {
        println("Directory already exists: $dir")
    }

    // Create database file
    val dbFilePath = Paths.get(dir, dbFilename)
    if (!Files.exists(dbFilePath)) {
        Files.createFile(dbFilePath)
        println("Database file created: $dbFilePath")
    } else {
        println("Database file already exists: $dbFilePath")
    }
}
