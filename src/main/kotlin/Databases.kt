package com.programmersbox.otakuworld

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabases() {
    val database = Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = "",
    )
    val userService = UserService(database)
    routing {
        post("/otaku/favorites") {
            val model = call.receive<DbModel>()
            println(model)
            val inserting = userService.create(model)
            println(inserting)
            call.respond(HttpStatusCode.Created)
        }

        get("/otaku/favorites/{type}") {
            val type = call.parameters["type"] ?: throw IllegalArgumentException("Invalid type")
            val models = userService.getModels(type)
            call.respond(HttpStatusCode.OK, models)
        }

        delete("/otaku/favorites") {
            val url = call.receive<DbModel>().url
            val numberDeleted = userService.deleteModel(url)
            call.respond(HttpStatusCode.OK, DeleteCount(numberDeleted))
        }

        post("/otaku/chapters") {
            val chapterWatched = call.receive<ChapterWatched>()
            val url = userService.addChapterWatched(chapterWatched)
            call.respond(HttpStatusCode.Created)
        }

        get("/otaku/chapters") {
            val favoriteUrl = call.receive<DbModel>().url
            val chaptersWatched = userService.getChapterWatched(favoriteUrl)
            call.respond(HttpStatusCode.OK, chaptersWatched)
        }

        delete("/otaku/chapters") {
            val url = call.receive<ChapterWatched>().url
            val numberDeleted = userService.removeChapterWatched(url)
            call.respond(HttpStatusCode.OK, DeleteCount(numberDeleted))
        }
    }
}

@Serializable
data class DeleteCount(val count: Int)