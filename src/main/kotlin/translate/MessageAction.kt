package net.accel.kmt.translate

import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member

class MessageAction(val lines: List<String>, val sender: Member, val group: Group, val method: TranslationMethod)