package net.accel.kmt.command

import net.accel.kmt.PluginMain

/**
 * Everyone can execute these commands. They are not miral commands.
 * @param name command name, beginning with '/'
 */
abstract class AbstractCommand(val name: String) {
    abstract fun execute(plugin: PluginMain, source: CommandSource, args: List<String>, followingLines: List<String>)
}