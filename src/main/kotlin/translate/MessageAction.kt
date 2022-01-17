package net.accel.kmt.translate

class MessageAction(
    val lines: List<String>,
    val method: TranslationMethod,
    val successCallback: (result: String) -> Unit
)