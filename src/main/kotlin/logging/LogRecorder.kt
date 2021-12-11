package net.accel.kmt.logging

import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.data.FlashImage
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.MessageChain
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.logging.*
import java.util.logging.Formatter

class LogRecorder(val logFile: Path, val imageDir: Path) {
    private var running: Boolean = false
    private var thread: Thread? = null

    private val msgQueue: BlockingQueue<MessageAndSender> = LinkedBlockingQueue()

    private val pLogger: Logger = Logger.getLogger("net.accel.kmt.logging.Logger")

    var client: HttpClient? = null

    private fun millsToString(m: Long): String {
        val date = Date(m)
        val fmt = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
        return fmt.format(date)
    }

    init {
        if (!Files.isDirectory(imageDir))
            throw RuntimeException("invalid image dir")

        LogManager.getLogManager().reset()

        val fhandler = FileHandler(logFile.toString(), 16777216, 5, true)
        fhandler.level = Level.INFO
        fhandler.formatter = object : Formatter() {
            override fun format(record: LogRecord?): String {
                val sb = StringBuilder()
                sb.append('[')
                sb.append(millsToString(record!!.millis))
                sb.append("] [")
                sb.append(record!!.level.name)
                sb.append("] ")
                sb.append(record!!.message)
                sb.append('\n')
                return sb.toString()
            }
        }
        pLogger.addHandler(fhandler)
    }

    fun start() {
        synchronized(this) {
            if (!running) {
                thread = Thread(Runnable {
                    run()
                })
                running = true
                thread!!.start()
            }
        }
    }

    fun stop() {
        synchronized(this) {
            if (running) {
                running = false
                thread!!.join()
                thread = null
            }
        }
    }

    private fun downloadImage(url: String, name: String) {
        if (client == null) {
            client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()
        }

        val targetPath = imageDir.resolve(name)
        if (Files.exists(targetPath))
            return

        val request = HttpRequest.newBuilder(URI.create(url))
            .GET()
            .header("User-Agent", "QQ")
            .build()

        client!!.send(request, HttpResponse.BodyHandlers.ofFile(targetPath))
    }

    companion object {
        val cbMap = "0123456789ABCDEF"
        private fun bytesToString(b: ByteArray): String {
            val sb = StringBuilder(b.size * 2)
            b.forEach {
                sb.append(cbMap[(it.toInt() and 0xFF) / 16])
                sb.append(cbMap[(it.toInt() and 0xFF) % 16])
            }
            return sb.toString()
        }

        private fun escapeString(s: String): String {
            val sb = StringBuilder(s.length)
            s.forEach {
                if (it == '\\')
                    sb.append("\\\\")
                else if (it == '\n')
                    sb.append("\\n")
                else
                    sb.append(it)
            }
            return sb.toString()
        }
    }

    fun insertMessage(chain: MessageChain, sender: Member) {
        msgQueue.offer(MessageAndSender(chain, sender))
    }

    fun run() {
        while (running) {
            val m = msgQueue.poll(1, TimeUnit.MILLISECONDS);
            if (m == null)
                continue

            val qid: Long = m.sender.id

            pLogger.info("[TXT] [$qid] " + escapeString(m.msg.contentToString()))
            m.msg.forEach {
                if (it is Image) {
                    val md5str = bytesToString(it.md5)
                    pLogger.info("[IMG] [$qid] $md5str")
                    runBlocking {
                        downloadImage(it.queryUrl(), md5str)
                    }
                } else if (it is FlashImage) {
                    val md5str = bytesToString(it.image.md5)
                    pLogger.info("[IMG] [$qid] $md5str")
                    runBlocking {
                        downloadImage(it.image.queryUrl(), md5str)
                    }
                }
            }
        }
    }
}