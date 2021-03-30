package dev.atsushieno.alsakt

class AlsaException : Exception {
    constructor() : super("ALSA error")
    constructor(errorCode: Int) : super("ALSA error (error code $errorCode)")
    constructor(msg: String?) : super(msg)

    constructor(msg: String?, innerException: Exception?) : super(msg, innerException)
}