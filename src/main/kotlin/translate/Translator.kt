package net.accel.kmt.translate

interface Translator {
    @Throws(TranslatingException::class)
    suspend fun translate(lines: List<String>, method: TranslationMethod) : String
}