package com.programmersbox.otakuworld.databases

import com.programmersbox.otakuworld.databases.UserService.ChapterWatchedModels
import com.programmersbox.otakuworld.databases.UserService.DbModels
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    //Modify this for choice of database and where it is located
    val database = Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = "",
    )

    transaction(database) {
        SchemaUtils.create(
            DbModels,
            ChapterWatchedModels,
            ListSchema.CustomListItemModel,
            ListSchema.CustomListModel,
        )
    }

    routing {
        //Use this for authentication
        /*authenticate {
            favorites(UserService(database))
            lists(ListSchema(database))
        }*/

        favorites(UserService(database))
        lists(ListSchema(database))
    }
}

private fun Routing.lists(listSchema: ListSchema) {
    post("/otaku/lists") {
        val list = call.receive<CustomListItem>()
        listSchema.createList(list)
        call.respond(HttpStatusCode.Created)
    }

    post("/otaku/lists/item") {
        val list = call.receive<CustomListInfo>()
        listSchema.addItem(list)
        call.respond(HttpStatusCode.Created)
    }

    get("/otaku/lists") {
        val list = listSchema.getAllLists()
        call.respond(HttpStatusCode.OK, list)
    }

    get("/otaku/lists/{id}") {
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid id")
        val list = listSchema.getList(id)
        call.respond(HttpStatusCode.OK, list)
    }

    delete("/otaku/lists/{id}") {
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid id")
        val items = listSchema.deleteList(id)
        call.respond(HttpStatusCode.OK, DeleteCount(items))
    }

    delete("/otaku/lists/all/{id}") {
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid id")
        val items = listSchema.deleteFullList(id)
        call.respond(HttpStatusCode.OK, DeleteCount(items))
    }
}

private fun Routing.favorites(userService: UserService) {
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
        userService.addChapterWatched(chapterWatched)
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

@Serializable
data class DeleteCount(val count: Int)