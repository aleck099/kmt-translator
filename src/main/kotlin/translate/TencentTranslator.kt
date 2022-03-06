package net.accel.kmt.translate

import com.tencentcloudapi.common.Credential
import com.tencentcloudapi.common.exception.TencentCloudSDKException
import com.tencentcloudapi.common.profile.ClientProfile
import com.tencentcloudapi.common.profile.HttpProfile
import com.tencentcloudapi.tmt.v20180321.TmtClient
import com.tencentcloudapi.tmt.v20180321.models.TextTranslateRequest
import com.tencentcloudapi.tmt.v20180321.models.TextTranslateResponse
import net.mamoe.mirai.utils.MiraiLogger
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.log

class TencentTranslator(
    private val APPID: String,
    private val APPKEY: String,
    private val PROJID: Long,
    private val logger: MiraiLogger
) : Translator {
    private val msgQueue: BlockingQueue<MessageAction> = LinkedBlockingQueue()
    private var thread = Thread()
    private val credential = Credential(APPID, APPKEY)
    private val profile = HttpProfile()
    private val clientProfile = ClientProfile("HmacSHA256", profile)
    private val client: TmtClient

    private var running: Boolean = false

    init {
        profile.reqMethod = "POST"
        profile.connTimeout = 6
        profile.writeTimeout = 6
        profile.readTimeout = 6
        client = TmtClient(credential, "ap-shanghai", clientProfile)
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

    override fun startTranslating(m: MessageAction) {
        synchronized(this) {
            if (running)
                msgQueue.offer(m)
        }
    }

    private fun buildRequest(msg: MessageAction): TextTranslateRequest {
        val textBuilder = StringBuilder()
        msg.lines.forEach {
            textBuilder.append(it)
            textBuilder.append('\n')
        }

        textBuilder.deleteCharAt(textBuilder.length - 1)

        val request = TextTranslateRequest()
        request.sourceText = textBuilder.toString()
        request.source = msg.method.langFrom
        request.target = msg.method.langTo
        request.projectId = PROJID
        return request
    }

    private fun run() {
        try {
            msgQueue.clear()
            var nextSendTime = 0L
            while (running) {
                val m = msgQueue.poll(1, TimeUnit.MILLISECONDS) ?: continue

                val request = buildRequest(m)
                val response: TextTranslateResponse
                while (nextSendTime > System.currentTimeMillis())
                    Thread.sleep(2)
                try {
                    response = client.TextTranslate(request)
                } catch (e: TencentCloudSDKException) {
                    logger.warning(e)
                    continue
                } catch (e: Throwable) {
                    logger.warning("UNKNOWN ERROR: $e")
                    continue
                }
                logger.info("request sent")
                nextSendTime = System.currentTimeMillis() + 201 // 5 requests in one second
                m.successCallback.invoke(response.targetText)
            }
        } catch (e: Throwable) {
            logger.warning(e)
        }
    }
}