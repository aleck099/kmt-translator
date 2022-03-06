package net.accel.kmt.command

class ExecutionResult {
    val command: AbstractCommand?
    val success: Boolean

    constructor(command: AbstractCommand?) {
        this.command = command
        this.success = command != null
    }

    constructor(command: AbstractCommand?, success: Boolean) {
        this.command = command
        this.success = success
    }
}