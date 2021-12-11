package net.accel.kmt

import kotlinx.coroutines.CompletableJob
import net.accel.kmt.logging.LogRecorder
import net.accel.kmt.translate.MessageAction
import net.accel.kmt.translate.TranslationMethod
import net.accel.kmt.translate.Translator
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText
import java.nio.file.Paths
import java.util.*
import java.util.regex.Pattern

/**
 * 使用 kotlin 版请把
 * `src/main/resources/META-INF.services/net.mamoe.mirai.console.plugin.jvm.JvmPlugin`
 * 文件内容改成 `org.example.mirai.plugin.PluginMain` 也就是当前主类全类名
 *
 * 使用 kotlin 可以把 java 源集删除不会对项目有影响
 *
 * 在 `settings.gradle.kts` 里改构建的插件名称、依赖库和插件版本
 *
 * 在该示例下的 [JvmPluginDescription] 修改插件名称，id和版本，etc
 *
 * 可以使用 `src/test/kotlin/RunMirai.kt` 在 ide 里直接调试，
 * 不用复制到 mirai-console-loader 或其他启动器中调试
 */

object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "net.accel.kmt.translator",
        name = "kmt-translator",
        version = "1.1"
    ) {
        author("aleck099")
        info("指令启动自动翻译聊天内容".trimIndent())
    }
) {
    private var translator: Translator? = null
    private var recorder: LogRecorder? = null
    private val spacePattern = Pattern.compile(" ")
    private val whitelist = ArrayList<Long>()

    private val listeners: Array<CompletableJob?> = Array(4) { null }

    private fun toLines(chain: MessageChain): LinkedList<String> {
        val lines = LinkedList<String>()
        chain.forEach {
            if (it is PlainText) {
                val content = it.content
                content.lines().forEach { it2 ->
                    if (it2.isNotEmpty())
                        lines.add(it2)
                }
            }
        }
        return lines
    }

    // 0 = Do nothing
    // 1 = C - J
    // 2 = J - C
    private fun processCommand(cmd: String): Int {
        val elements = cmd.split(spacePattern)
        if (elements.isEmpty()) return 0
        if (elements[0] == "/tr") {
            if (elements.size == 1) return 1
            val arg1 = elements[1]
            return if (arg1 == "j") 1 else if (arg1 == "c") 2 else 0
        }
        return 0
    }

    private fun identify(lines: List<String>, sender: Member): TranslationMethod? {
        if (lines.isEmpty()) return null
        if (sender.id in whitelist) return TranslationMethod("jp", "zh", true)
        return when (processCommand(lines[0])) {
            0 -> null
            1 -> TranslationMethod("zh", "jp", false)
            2 -> TranslationMethod("jp", "zh", false)
            else -> throw RuntimeException("assertion failed")
        }
    }

    override fun onEnable() {
        Config.reload()
        translator = Translator(Config.appid, Config.appkey, logger)
        translator!!.start()
        recorder = LogRecorder(Paths.get(dataFolder.absolutePath, Config.logfile),
            Paths.get(dataFolder.absolutePath, Config.imagedir))
        recorder!!.start()
        whitelist.clear()
        whitelist.addAll(Config.whitelist)
        //配置文件目录 "${dataFolder.absolutePath}/"
        val eventChannel = GlobalEventChannel.parentScope(this)
        listeners[0] = eventChannel.subscribeAlways<GroupMessageEvent> {
            // 群消息
            logger.info("group message")
            recorder!!.insertMessage(message, sender)
            val lines = toLines(message)
            val result = identify(lines, sender)
            if (result != null) {
                logger.info("starting translating")
                if (!result.isWhitelisted) {
                    lines.removeFirst()
                }
                translator!!.startTranslating(MessageAction(lines, sender, group, result))
            }
        }
        listeners[1] = eventChannel.subscribeAlways<FriendMessageEvent>{
            //好友信息
            logger.info("friend message")
            this.sender.sendMessage("RECEIVED")
        }
        listeners[2] = eventChannel.subscribeAlways<NewFriendRequestEvent>{
            //不同意好友申请
            reject()
        }
        listeners[3] = eventChannel.subscribeAlways<BotInvitedJoinGroupRequestEvent>{
            //同意加群申请
            logger.info("join group")
            accept()
        }
        logger.info("plugin enabled")
    }

    override fun onDisable() {
        translator!!.waitStop()
        translator = null
        recorder!!.stop()
        recorder = null
        for (l in listeners) {
            l!!.complete()
        }
    }
}

object Config: AutoSavePluginConfig("kmt-translator") {
    val appid by value<String>()
    val appkey by value<String>()
    val logfile by value<String>()
    val imagedir by value<String>()
    val whitelist: List<Long> by value()
}
