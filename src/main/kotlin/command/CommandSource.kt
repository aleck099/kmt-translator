package net.accel.kmt.command

import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.MessageChain

/**
 * Identify a command executor.
 * @param sender the user whe executes this command, maybe a friend or someone in a group
 * @param group representing that group, may be null
 * @param message raw message
 */
class CommandSource(val sender: User, val group: Group?, val message: MessageChain)