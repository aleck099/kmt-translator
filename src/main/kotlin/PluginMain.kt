package net.accel.kmt

import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.runBlocking
import net.accel.kmt.command.CommandSource
import net.accel.kmt.command.Commands
import net.accel.kmt.command.TranslateCommand
import net.accel.kmt.translate.MessageAction
import net.accel.kmt.translate.TencentTranslator
import net.accel.kmt.translate.TranslationMethod
import net.accel.kmt.translate.Translator
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.source
import java.util.regex.Pattern

object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "net.accel.kmt.translator",
        version = "1.5"
    ) {
        author("aleck099")
        info("指令启动自动翻译聊天内容")
    }
) {
    var translator: Translator = TencentTranslator(Config.appid, Config.appkey, 0, logger)
    val whitelist = ArrayList<Long>()
    val group_whitelist = ArrayList<Long>()
    val commands = Commands()

    private val listeners: Array<CompletableJob?> = Array(4) { null }

    init {
        commands.register(TranslateCommand())
    }

    override fun onEnable() {
        Config.reload()
        // translator = BaiduTranslator(Config.appid, Config.appkey, logger)
        translator = TencentTranslator(Config.appid, Config.appkey, 0, logger)
        translator.start()
        whitelist.clear()
        whitelist.addAll(Config.whitelist)
        group_whitelist.clear()
        group_whitelist.addAll(Config.group_whitelist)

        val eventChannel = GlobalEventChannel.parentScope(this)
        listeners[0] = eventChannel.subscribeAlways<GroupMessageEvent> {
            if (group.id !in group_whitelist)
                return@subscribeAlways
            val lines = Commands.messageToLines(message)
            if (lines.isEmpty())
                return@subscribeAlways
            val firstLine = lines.first()
            val followingLines = lines.drop(1)
            val result =
                commands.execute(this@PluginMain, CommandSource(sender, group, message), firstLine, followingLines)
            if (result.command == null) {
                // whitelist
                if (sender.id in whitelist) {
                    translator.startTranslating(MessageAction(lines, TranslationMethod("ja", "zh", true)) {
                        runBlocking {
                            group.sendMessage(
                                MessageChainBuilder().append(QuoteReply(message.source)).append(PlainText(it))
                                    .build()
                            )
                        }
                    })
                }
            }

        }
        listeners[1] = eventChannel.subscribeAlways<FriendMessageEvent> {
            val lines = Commands.messageToLines(message)
            if (lines.isEmpty())
                return@subscribeAlways
            val firstLine = lines.first()
            val followingLines = lines.drop(1)
            commands.execute(this@PluginMain, CommandSource(sender, null, message), firstLine, followingLines)
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
        translator.waitStop()
    }
}

object Config : AutoSavePluginConfig("kmt-translator") {
    val appid by value<String>()
    val appkey by value<String>()
    val whitelist: List<Long> by value()
    val group_whitelist: List<Long> by value()
}
