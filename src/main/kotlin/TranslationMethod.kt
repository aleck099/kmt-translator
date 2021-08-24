package net.accel.kmt

class TranslationMethod(lF: String, lT: String, isWhitelisted: Boolean) {
    val langFrom: String
    val langTo: String
    val isWhitelisted: Boolean

    init {
        langFrom = lF
        langTo = lT
        this.isWhitelisted = isWhitelisted
    }
}