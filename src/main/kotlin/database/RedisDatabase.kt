package database

import Cache.CacheValue
import Cache.RedisCache
import config.RedisConfig
import java.io.File
import java.io.FileInputStream
import java.io.PushbackInputStream

class RedisDatabase(val redisConfig: RedisConfig) {
    var exists = false
    var validHeader = false
    var validMedata = false
    var validDatabase = false
    val databases = mutableMapOf<Int, RedisCache>()

    fun read() {
        val file = File(redisConfig.dbPath())
        if (!file.exists()) {
            this.exists = false
            return
        }

        val inputStream = FileInputStream(file)
        readHeader(inputStream)
        exists = true
        validHeader = true
        readMetadata(inputStream)
        validMedata = true
        readDatabases(inputStream)
        validDatabase = true
    }

    /*
    * Database section
    * */

    fun readDatabases(inputStream: FileInputStream) {
        /* The index of the database (size encoded).
                          Here, the index is 0. */
        val dbIndex = inputStream.read()
        val db = RedisCache()
        databases.put(dbIndex, db)

        // Indicates that hash table size information follows.
        assert(inputStream.read() == 0xFB)

        /* The size of the hash table that stores the keys and values (size encoded).
                         Here, the total key-value hash table size is 3. */
        val hashTableSize = readSizeEncoding(inputStream)
        /* The size of the hash table that stores the expires of the keys (size encoded).
                       Here, the number of keys with an expiry is 2. */
        val expiryTableSize = readSizeEncoding(inputStream)

        val map = mutableMapOf<String, CacheValue>()
        val cacheValue = null
        for (i in 0 until hashTableSize) {
            val encoding = inputStream.read()
            val key = readEncodedString(inputStream)
            if (key == null) {
                throw Exception("Invalid file format")
            }
            val value = readEncodedString(inputStream)
            if (value == null) {
                throw Exception("Invalid file format")
            }
            var cacheValue = CacheValue(key, value, -1)
            if (peekByte(inputStream) == 0xFD) {
                val expire = read32bitInteger(inputStream)
                cacheValue = CacheValue(key, value, expire.toLong())
            }
            db.add(cacheValue)
        }

        System.out.println("Database index: $hashTableSize")
    }

    /*
    * Size Encoded parsing
    * */
    fun readSizeEncoding(inputStream: FileInputStream): Int {
        val length = inputStream.read()
        /* If the first two bits are 0b00:
       The size is the remaining 6 bits of the byte.
       In this example, the size is 10: */
        if ((0xC0 and length) == 0x00) {
            return extract6BitFlag(length)
        }
        /* If the first two bits are 0b01:
       The size is the next 14 bits
       (remaining 6 bits in the first byte, combined with the next byte),
       in big-endian (read left-to-right).
       In this example, the size is 700: */
        else if ((0xC0 and length == 0x40)) {
            return extract14BitFlag(length, inputStream)
        }
        /* If the first two bits are 0b10:
       Ignore the remaining 6 bits of the first byte.
       The size is the next 4 bytes, in big-endian (read left-to-right).
       In this example, the size is 17000: */
        else if ((0xC0 and length) == 0x80) {
            return read32bitInteger(inputStream).toInt()
        }
        /* If the first two bits are 0b11:
       The remaining 6 bits specify a type of string encoding.
       See string encoding section. */
        else if (0xC0 and length == 0xC0) {
            return -1;
        } else {
            return length
        }
    }

    /*
    * Metadata section
    *
    * */

    val START_OF_METADATA_SECTION = 0xFA
    val END_OF_METADATA_SECTION = 0xFE // Also database start section

    fun readMetadata(inputStream: FileInputStream):List<String> {
        if (inputStream.read() != START_OF_METADATA_SECTION) {
            throw Exception("Invalid file format")
        }

        val metaSection = mutableListOf<String>()

        while (true) {
            val value = readEncodedString(inputStream)
            if (value == null) {
                break
            }
            metaSection.add(value)
        }

        return metaSection
    }

    /*
    * String encoded parsing
    * */
    fun readEncodedString(inputStream: FileInputStream, length: Int): String? {
        if (length == 0xC0) {
            return read8bitInteger(inputStream)
        } else if (length == 0xC2) {
            return read32bitInteger(inputStream)
        } else if (length == 0xC1) {
            return read16bitInteger(inputStream)
        } else {
            return readMetaSection(inputStream, length)
        }
    }
    fun readEncodedString(inputStream: FileInputStream): String? {
        var length = inputStream.read()
        if (length == END_OF_METADATA_SECTION) {
            return null;
        }
        if (length == START_OF_METADATA_SECTION) {
            length = inputStream.read()
        }
        return readEncodedString(inputStream, length)
    }

    val NEW_LINE = 0xA
    private fun readMetaSection(inputStream: FileInputStream, length: Int): String {
        var value = ""
        for (i in 0 until length) {
            val key = inputStream.read()
            value += key.toChar()
        }
        return value
    }

    /*
    * Header section
    * */
    private fun readHeader(inputStream: FileInputStream) {
        val expected = "REDIS0011"
        for (i in expected.indices) {
            val b = inputStream.read()
            if (b != expected[i].code) {
                throw Exception("Invalid header version format")
            }
        }
    }

    /*
    * Utility functions
    * */
    fun peekByte(inputStream: FileInputStream): Int {
        val pushbackInputStream = PushbackInputStream(inputStream)
        val byte = pushbackInputStream.read()
        pushbackInputStream.unread(byte)
        return byte
    }

    fun extract6BitFlag(byte: Int): Int {
        return byte and 0x3F
    }

    fun extract14BitFlag(byte: Int, inputStream: FileInputStream): Int {
        // extract first 6 bits from current byte
        val currentByte = extract6BitFlag(byte)
        val nextByte = inputStream.read() shl 8
        return (nextByte or currentByte)
    }

    private fun read8bitInteger(inputStream: FileInputStream): String {
        return inputStream.read().toString()
    }

    private fun read16bitInteger(inputStream: FileInputStream): String {
        val byte0 = inputStream.read()
        val byte1 = inputStream.read() shl 8
        return (byte1 or byte0).toString()
    }

    private fun read32bitInteger(inputStream: FileInputStream): String {
        val byte0 = inputStream.read()
        val byte1 = inputStream.read() shl 8
        val byte2 = inputStream.read() shl 16
        val byte3 = inputStream.read() shl 24
        return (byte3 or byte2 or byte1 or byte0).toString()
    }
}
