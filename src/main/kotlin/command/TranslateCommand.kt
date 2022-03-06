package net.accel.kmt.command

import kotlinx.coroutines.runBlocking
import net.accel.kmt.PluginMain
import net.accel.kmt.translate.MessageAction
import net.accel.kmt.translate.TranslationMethod
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.source

class TranslateCommand() : AbstractCommand("/tr") {
    companion object {
        val languages = arrayOf("zh", "ja")
    }

    override fun execute(plugin: PluginMain, source: CommandSource, args: List<String>, followingLines: List<String>) {
        val method: TranslationMethod
        if (args.size == 2) {
            val lang2: Array<String?> = arrayOf(null, null)
            for ((index, e) in args.withIndex()) {
                if (e !in languages)
                    throw CommandException("unsupported language")
                lang2[index] = e
            }
            method = TranslationMethod(lang2[0]!!, lang2[1]!!, false)
        } else if (args.isEmpty()) {
            method = TranslationMethod("zh", "ja", false)
        } else {
            throw CommandException("wrong parameter count")
        }

        if (followingLines.isNotEmpty()) {
            val action = MessageAction(followingLines, method) {
                runBlocking {
                    if (source.group == null) {
                        source.sender.sendMessage(
                            MessageChainBuilder()
                                .append(PlainText(it))
                                .build()
                        )
                    } else {
                        source.group.sendMessage(
                            MessageChainBuilder()
                                .append(QuoteReply(source.message.source))
                                .append(PlainText(it))
                                .build()
                        )
                    }
                }
            }
            plugin.translator.startTranslating(action)
        }
    }

}