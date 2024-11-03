package command

import java.io.BufferedReader

class Command(
    private val reader: BufferedReader
) {
    val words: List<String>
    init {
        words = params(this.reader)    // get emptyList in case connection remains open
    }

    fun isValid(): Boolean {
        return words.isNotEmpty()
    }

    fun name(): String {
        return words.get(0)
    }

    fun params(): List<String> {
        return words.drop(1)
    }

    private fun params(reader: BufferedReader): List<String> {
        val starChar = this.reader.read() // reading first char from RESP input string
        if (starChar == -1) return emptyList()
        val n = readUntilEnd().toInt()

        val commands = mutableListOf<String>()
        for (i in 0 until n) {
            val command = readStr()
            commands.add(command)
        }

        return commands
    }

    private fun readUntilEnd(): String {
        val msg = StringBuilder()

        while(!msg.endsWith("\r\n") && !msg.endsWith("\n")) {
            val c = this.reader.read()
            if (c == -1) break
            msg.append(c.toChar())

//            println("" + c.toInt() + " [" + c.toChar() + "]")
        }
        return msg.toString().replace("\r\n", "")
    }

    private fun readStr(): String {
        val strLen = readUntilEnd().drop(1).toInt()
        val str = readStrMsg(strLen)
        return str
    }

    private fun readStrMsg(strLen: Int): String {
        var c = 0
        val str = StringBuilder()
        while (c < (strLen + 2)) { // strLen + 2, for reading those extra \r\n
            val ch = this.reader.read()
            if (ch == -1) break
            str.append(ch.toChar())
            c++
        }
        return str.toString().replace("\r\n", "")
    }
}
