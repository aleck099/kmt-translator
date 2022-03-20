package net.accel.kmt

import kotlinx.coroutines.CompletableJob
import net.accel.kmt.translate.TencentTranslator
import net.accel.kmt.translate.TranslatingException
import net.accel.kmt.translate.TranslationMethod
import net.accel.kmt.translate.Translator
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.message.data.*
import java.util.*

object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "net.accel.kmt.translator",
        version = "1.6"
    ) {
        author("aleck099")
        info("指令启动自动翻译聊天内容")
    }
) {
    var translator: Translator = TencentTranslator(Config.appid, Config.appkey, 0, logger)
    val whitelist = ArrayList<Long>()
    val group_whitelist = ArrayList<Long>()

    private val listeners: Array<CompletableJob?> = Array(4) { null }

    fun messageToLines(m: MessageChain): List<String> {
        val lines = ArrayList<String>()
        m.forEach {
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

    fun parseCommand(s: String) : Optional<List<String>> {
        val args = s.split(' ', limit = 3)
        if (args.isEmpty()) return Optional.empty()
        if (args[0] != "/tr") return Optional.empty()

        return if (args.size == 3)
            Optional.of(arrayListOf(args[1], args[2]))
        else
            Optional.of(arrayListOf())
    }

    private suspend fun sendResult(target: Contact, source: MessageSource, str: String) {
        target.sendMessage(MessageChainBuilder()
            .append(QuoteReply(source))
            .append(PlainText(str))
            .build())
    }

    suspend fun translateWithRetry(target: Contact, source: MessageSource, lines: List<String>, method: TranslationMethod) {
        var ep: Throwable? = null
        for (i in 0 until 3) {
            try {
                sendResult(target, source, translator.translate(lines, method))
                return
            } catch (e: TranslatingException) {
                ep = e
            }
        }
        throw ep!!
    }

    override fun onEnable() {
        logger.info("kmt-translator ${description.version}")
        Config.reload()
        // translator = BaiduTranslator(Config.appid, Config.appkey, logger)
        translator = TencentTranslator(Config.appid, Config.appkey, 0, logger)
        whitelist.clear()
        whitelist.addAll(Config.whitelist)
        group_whitelist.clear()
        group_whitelist.addAll(Config.group_whitelist)

        val eventChannel = GlobalEventChannel.parentScope(this)
        listeners[0] = eventChannel.subscribeAlways<GroupMessageEvent> {
            if (group.id !in group_whitelist)
                return@subscribeAlways
            val lines = messageToLines(message)
            if (lines.isEmpty())
                return@subscribeAlways
            val args = parseCommand(lines.first())
            val followingLines = lines.drop(1)
            try {
                if (args.isEmpty) {
                    // whitelist
                    if (sender.id in whitelist)
                        translateWithRetry(group, message.source, lines, TranslationMethod("ja", "zh"))
                } else {
                    val innerArgs = args.get()
                    val method = if (innerArgs.isEmpty())
                        TranslationMethod("zh", "ja")
                    else
                        TranslationMethod(innerArgs[0], innerArgs[1])
                    translateWithRetry(group, message.source, followingLines, method)
                }
            } catch (e: TranslatingException) {
                logger.warning(e)
            }
        }
        listeners[1] = eventChannel.subscribeAlways<FriendMessageEvent> {
            val lines = messageToLines(message)
            if (lines.isEmpty())
                return@subscribeAlways
            val args = parseCommand(lines.first())
            if (args.isEmpty || args.get().isEmpty()) return@subscribeAlways
            val followingLines = lines.drop(1)
            translateWithRetry(sender, message.source, followingLines, TranslationMethod(args.get()[0], args.get()[1]))
        }
        listeners[2] = eventChannel.subscribeAlways<NewFriendRequestEvent> {
            logger.info("add friend")
            accept()
        }
        listeners[3] = eventChannel.subscribeAlways<BotInvitedJoinGroupRequestEvent> {
            logger.info("join group")
            accept()
        }
        logger.info("plugin enabled")
    }

    override fun onDisable() {
        for (l in listeners) {
            l!!.complete()
        }
    }
}

object Config : AutoSavePluginConfig("kmt-translator") {
    val appid by value<String>()
    val appkey by value<String>()
    val whitelist: List<Long> by value()
    val group_whitelist: List<Long> by value()
}
