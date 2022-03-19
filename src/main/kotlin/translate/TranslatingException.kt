package net.accel.kmt.translate

class TranslatingException: Exception {
    constructor() : super()
    constructor(s: String) : super(s)
    constructor(e: Throwable): super(e)
}