package net.accel.kmt.translate

import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
import java.util.concurrent.TimeUnit

class BaiduTranslator(private val APPID: String, private val APPKEY: String, private val logger: MiraiLogger) :
    Translator {
    private val client: HttpClient
    private val randNum: Int
    private var lastSubmissionTime: Long = 0

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

    private fun buildRequestBody(lines: List<String>, method: TranslationMethod): BodyPublisher {
        val textBuilder = StringBuilder()
        lines.forEach {
            textBuilder.append(it)
            textBuilder.append('\n')
        }
        textBuilder.deleteCharAt(textBuilder.length - 1)
        val text = textBuilder.toString()
        val r = StringBuilder(512)
        r.append("q=")
        r.append(URLEncoder.encode(text, StandardCharsets.UTF_8))
        r.append("&from=")
        r.append(method.langFrom)
        r.append("&to=")
        r.append(method.langTo)
        r.append("&appid=")
        r.append(APPID)
        r.append("&salt=")
        r.append(randNum)
        r.append("&sign=")
        r.append(sign(text))
        return HttpRequest.BodyPublishers.ofByteArray(r.toString().toByteArray(StandardCharsets.UTF_8))
    }

    private fun decodeResult(jsonResult: String): String {
        return try {
            val e = JsonParser.parseString(jsonResult)
            val root = e.asJsonObject
            val errorCode = root["error_code"]
            if (errorCode != null && errorCode.asInt != 52000) {
                throw TranslatingException("remote API returned error")
            }
            val sb = StringBuilder()
            val results = root["trans_result"].asJsonArray
            for (i in 0 until results.size()) {
                sb.append(results[i].asJsonObject["dst"].asString)
            }
            sb.toString()
        } catch (e: JsonParseException) {
            throw TranslatingException(e)
        } catch (e: IllegalStateException) {
            throw TranslatingException(e)
        }
    }

    override suspend fun translate(lines: List<String>, method: TranslationMethod): String = withContext(Dispatchers.IO) {
        val body = buildRequestBody(lines, method)
        val request = HttpRequest.newBuilder(URI.create(API))
            .POST(body)
            .version(HttpClient.Version.HTTP_1_1)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("User-Agent", "miral")
            .build()

        val currentMillis = System.currentTimeMillis()
        val delayMillis = lastSubmissionTime + 1100 - currentMillis
        if (delayMillis > 0) {
            lastSubmissionTime = currentMillis + delayMillis
            delay(delayMillis)
        } else {
            lastSubmissionTime = currentMillis
        }

        logger.info("sending request")
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        /* return */ decodeResult(response.body())
    }

}