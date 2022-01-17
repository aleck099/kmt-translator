package net.accel.kmt.command

import net.accel.kmt.PluginMain
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText
import java.util.*

class Commands {
    companion object {
        fun messageToLines(m: MessageChain): List<String> {
            val lines = LinkedList<String>()
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
    }

    private val commandMap = TreeMap<String, AbstractCommand>()

    fun register(command: AbstractCommand) {
        commandMap[command.name] = command
    }

    fun execute(plugin: PluginMain, source: CommandSource, rawCommand: String, followingLines: List<String>): ExecutionResult {
        var state = 0
        val args = LinkedList<String>()
        val currentArg = StringBuffer()
        var index = 0
        while (index < rawCommand.length) {
            val c = rawCommand[index]
            when (state) {
                0 -> {
                    when (c) {
                        ' ' -> state = 1
                        '\\' -> state = 2
                        else ->  currentArg.append(c)
                    }
                    ++index
                }
                1 -> {
                    args.add(currentArg.toString())
                    currentArg.setLength(0)
                    state = 0
                }
                2 -> {
                    when (c) {
                        ' ', '\\' -> {
                            currentArg.append(c)
                            state = 0
                        }
                        else -> {
                            return ExecutionResult(null)
                        }
                    }
                }
            }
        }
        if (state == 2)
            return ExecutionResult(null)
        if (currentArg.isNotEmpty()) {
            args.add(currentArg.toString())
        }
        if (args.isEmpty())
            return ExecutionResult(null)
        val commandName = args.first
        args.removeFirst()
        val command = commandMap[commandName]
        if (command == null) {
            return ExecutionResult(null)
        } else try {
            command.execute(plugin, source, args, followingLines)
        } catch (e: CommandException) {
            plugin.logger.info("CMDFAIL(${command.name}): ${e.message}")
            return ExecutionResult(command, false)
        }
        return ExecutionResult(command, true)
    }
}