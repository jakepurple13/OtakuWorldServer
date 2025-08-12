package com.programmersbox.otakuworld

import com.programmersbox.otakuworld.databases.configureDatabases
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureFrameworks()
    configureSerialization()
    configureDatabases()
    configureMonitoring()
    configureSecurity()
    configureRouting()
}
