package net.accel.kmt.logging

import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.data.MessageChain

class MessageAndSender(val msg: MessageChain, val sender: Member) {
}