package net.accel.kmt.translate

interface Translator {
    fun start()
    fun waitStop()
    fun startTranslating(m: MessageAction)
}