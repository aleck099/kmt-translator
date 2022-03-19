package net.accel.kmt.translate

import com.tencentcloudapi.common.Credential
import com.tencentcloudapi.common.exception.TencentCloudSDKException
import com.tencentcloudapi.common.profile.ClientProfile
import com.tencentcloudapi.common.profile.HttpProfile
import com.tencentcloudapi.tmt.v20180321.TmtClient
import com.tencentcloudapi.tmt.v20180321.models.TextTranslateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.mamoe.mirai.utils.MiraiLogger

class TencentTranslator(
    private val APPID: String,
    private val APPKEY: String,
    private val PROJID: Long,
    private val logger: MiraiLogger
) : Translator {
    private val credential = Credential(APPID, APPKEY)
    private val profile = HttpProfile()
    private val clientProfile = ClientProfile("HmacSHA256", profile)
    private val client: TmtClient

    private var lastSubmissionTime: Long = 0

    init {
        profile.reqMethod = "POST"
        profile.connTimeout = 6
        profile.writeTimeout = 6
        profile.readTimeout = 6
        client = TmtClient(credential, "ap-shanghai", clientProfile)
    }

    private fun buildRequest(lines: List<String>, method: TranslationMethod): TextTranslateRequest {
        val textBuilder = StringBuilder()
        lines.forEach {
            textBuilder.append(it)
            textBuilder.append('\n')
        }

        textBuilder.deleteCharAt(textBuilder.length - 1)

        val request = TextTranslateRequest()
        request.sourceText = textBuilder.toString()
        request.source = method.langFrom
        request.target = method.langTo
        request.projectId = PROJID
        return request
    }

    override suspend fun translate(lines: List<String>, method: TranslationMethod): String = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest(lines, method)

            val currentMillis = System.currentTimeMillis()
            val delayMillis = lastSubmissionTime + 220 - currentMillis
            if (delayMillis > 0) {
                lastSubmissionTime = currentMillis + delayMillis
                delay(delayMillis)
            } else {
                lastSubmissionTime = currentMillis
            }

            logger.info("sending request")
            val response = client.TextTranslate(request)
            /* return */ response.targetText
        } catch (e: TencentCloudSDKException) {
            throw TranslatingException(e)
        }
    }

}