package net.accel.kmt.translate

import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.utils.MiraiLogger
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublisher
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class BaiduTranslator(private val APPID: String, private val APPKEY: String, private val logger: MiraiLogger) :
    Translator {
    private var client: HttpClient
    private val randNum: Int
    private val msgQueue: BlockingQueue<MessageAction> = LinkedBlockingQueue()

    private var running: Boolean = false
    private var thread = Thread()

    companion object {
        const val API = "https://fanyi-api.baidu.com/api/trans/vip/translate"
        val charMap = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
    }

    init {
        val builder = HttpClient.newBuilder()
        builder.followRedirects(HttpClient.Redirect.NORMAL)
        client = builder.build()
        randNum = Random().nextInt()
    }

    override fun start() {
        synchronized(this) {
            if (!running) {
                thread = Thread({ run() }, "translator")
                running = true
                thread.start()
            }
        }
    }

    override fun waitStop() {
        synchronized(this) {
            if (running) {
                running = false
                thread.join()
            }
        }
    }

    private fun hexToString(b: ByteArray): String {
        val bu = StringBuilder(32)
        for (i in b) {
            bu.append(charMap[(i.toInt() and 0xF0).shr(4)])
            bu.append(charMap[(i.toInt() and 0x0F)])
        }
        return bu.toString()
    }

    private fun sign(query: String): String {
        val fs = APPID + query + randNum + APPKEY
        val digest = MessageDigest.getInstance("MD5")
        val data = fs.toByteArray(Charsets.UTF_8)
        digest.update(data)
        val output = digest.digest()
        return hexToString(output)
    }

    override fun startTranslating(m: MessageAction) {
        synchronized(this) {
            if (running)
                msgQueue.offer(m)
        }
    }

    private fun buildRequestBody(msg: MessageAction): BodyPublisher {
        val textBuilder = StringBuilder()
        msg.lines.forEach {
            textBuilder.append(it)
            textBuilder.append('\n')
        }
        val text = textBuilder.toString()
        val r = StringBuilder(512)
        r.append("q=")
        r.append(URLEncoder.encode(text, StandardCharsets.UTF_8))
        r.append("&from=")
        r.append(msg.method.langFrom)
        r.append("&to=")
        r.append(msg.method.langTo)
        r.append("&appid=")
        r.append(APPID)
        r.append("&salt=")
        r.append(randNum)
        r.append("&sign=")
        r.append(sign(text))
        return HttpRequest.BodyPublishers.ofByteArray(r.toString().toByteArray(StandardCharsets.UTF_8))
    }

    private fun decodeResult(jsonResult: String): String? {
        return try {
            val e = JsonParser.parseString(jsonResult)
            val root = e.asJsonObject
            val errorCode = root["error_code"]
            if (errorCode != null && errorCode.asInt != 52000) {
                return null
            }
            val sb = StringBuilder()
            val results = root["trans_result"].asJsonArray
            for (i in 0 until results.size()) {
                sb.append(results[i].asJsonObject["dst"].asString)
            }
            sb.toString()
        } catch (e: JsonParseException) {
            null
        } catch (e: IllegalStateException) {
            null
        }
    }

    private fun run() {
        try {
            msgQueue.clear()
            var nextSendTime = 0L
            while (running) {
                val m = msgQueue.poll(1, TimeUnit.MILLISECONDS) ?: continue

                val body = buildRequestBody(m)
                val request = HttpRequest.newBuilder(URI.create(API))
                    .POST(body)
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", "miral")
                    .build()
                while (nextSendTime > System.currentTimeMillis())
                    Thread.sleep(2)
                val response: HttpResponse<String> = client.send<String>(request, HttpResponse.BodyHandlers.ofString())
                logger.info("request sent")
                nextSendTime = System.currentTimeMillis() + 1001 // one request per second
                val result = decodeResult(response.body())
                if (result != null) {
                    m.successCallback.invoke(result)
                } else {
                    logger.warning("decode failed")
                }
            }
        } catch (e: Throwable) {
            logger.warning(e)
        }
    }
}