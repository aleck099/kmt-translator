package net.accel.kmt.command

class CommandException : Exception {
    constructor() : super()
    constructor(s: String) : super(s)
    constructor(e: Throwable): super(e)
}