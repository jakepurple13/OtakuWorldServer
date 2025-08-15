package com.programmersbox.otakuworld.databases

import com.programmersbox.otakuworld.databases.UserService.ChapterWatchedModels
import com.programmersbox.otakuworld.databases.UserService.DbModels
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.sse.*
import io.ktor.util.logging.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.net.InetAddress
import java.net.NetworkInterface
import kotlin.time.Duration.Companion.seconds

fun Application.configureDatabases() {
    //Modify this for choice of database and where it is located
    /*val database = R2dbcDatabase.connect(
        url = "r2dbc:h2:mem:///test",
        //user = "root",
        //driver = "org.h2.Driver",
        //password = "",
    )*/
    /*val database = Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = "",
    )*/

    val database = Database.connect(
        url = "jdbc:h2:./myh2file:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = "",
    )

    transaction(db = database) {
        SchemaUtils.create(
            DbModels,
            ChapterWatchedModels,
            ListSchema.CustomListItemModel,
            ListSchema.CustomListModel,
        )
    }

    //println(getIP())

    install(SSE)

    routing {
        //Use this for authentication
        /*authenticate {
            databasing(log = log)
        }*/

        databasing(log = log)
    }
}

private fun Routing.databasing(log: Logger) {
    val updateLocal = MutableSharedFlow<CustomSSE>(0)

    sse("/otaku/sse") {
        println("Connected to SSE")
        heartbeat {
            period = 10000.seconds
        }
        try {
            updateLocal
                //.shareIn(this, SharingStarted.WhileSubscribed())
                .collect { data ->
                    //println("Sending SSE: $data")
                    send(
                        ServerSentEvent(
                            data = Json.encodeToString(data),
                            event = data.eventType.toString()
                        )
                    )
                }
        } catch (e: Exception) {
            log.error("Error during SSE collection: ${e.message}")
        }
        println("Disconnected from SSE")
    }

    favorites(
        userService = UserService(),
        updateLocal = updateLocal
    )
    lists(
        listSchema = ListSchema(),
        updateLocal = updateLocal
    )
}

private fun Routing.lists(listSchema: ListSchema, updateLocal: MutableSharedFlow<CustomSSE>) {
    post("/otaku/lists") {
        val list = call.receive<CustomListItem>()
        listSchema.createList(list)
        call.respond(HttpStatusCode.Created)
        updateLocal.emit(CustomSSE.AddEvent(EventType.ADD_LIST, list.uuid))
    }

    post("/otaku/lists/item") {
        val list = call.receive<CustomListInfo>()
        listSchema.addItem(list)
        call.respond(HttpStatusCode.Created)
        updateLocal.emit(CustomSSE.AddEvent(EventType.ADD_LIST_ITEM, list.uuid))
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

    patch("/otaku/lists") {
        val list = call.receive<CustomListItem>()
        listSchema.createList(list)
        call.respond(HttpStatusCode.Accepted)
        updateLocal.emit(CustomSSE.AddEvent(EventType.ADD_LIST_ITEM, list.uuid))
    }

    patch("/otaku/lists/biometric/{id}") {
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid id")
        val useBiometric = call.receive<Biometric>().useBiometric
        listSchema.updateBiometric(id, useBiometric)
        call.respond(HttpStatusCode.Accepted)
        updateLocal.emit(CustomSSE.AddEvent(EventType.ADD_LIST_ITEM, id))
    }

    get("/otaku/list/{id}") {
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid id")
        val list = listSchema.getListByUuid(id)
        call.respond(HttpStatusCode.OK, list)
    }

    delete("/otaku/lists/{id}") {
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid id")
        val items = listSchema.deleteList(id)
        call.respond(HttpStatusCode.OK, DeleteCount(items))
        updateLocal.emit(CustomSSE.DeleteEvent(EventType.REMOVE_LIST_ITEM, id))
    }

    delete("/otaku/lists/all/{id}") {
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid id")
        val items = listSchema.deleteFullList(id)
        call.respond(HttpStatusCode.OK, DeleteCount(items))
        updateLocal.emit(CustomSSE.DeleteEvent(EventType.REMOVE_LIST, id))
    }
}

@Serializable
data class Biometric(val useBiometric: Boolean)

private fun Routing.favorites(
    userService: UserService,
    updateLocal: MutableSharedFlow<CustomSSE>,
) {
    post("/otaku/favorites") {
        val model = call.receive<DbModel>()
        //println(model)
        val inserting = userService.create(model)
        //println(inserting)
        call.respond(HttpStatusCode.Created)
        updateLocal.emit(CustomSSE.AddEvent(EventType.NEW_FAVORITE, model.url))
    }

    get("/otaku/favorites/item") {
        val model = call.receive<String>()
        //println(model)
        val inserting = userService.getFavorite(model)
        //println(inserting)
        call.respond(inserting)
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
        updateLocal.emit(CustomSSE.DeleteEvent(EventType.DELETE_FAVORITE, url))
    }

    get("/otaku/chapters/item") {
        val url = call.receive<String>()
        val chapterWatched = userService.getChapterWatchedUrl(url)
        call.respond(chapterWatched)
    }

    post("/otaku/chapters") {
        val chapterWatched = call.receive<ChapterWatched>()
        userService.addChapterWatched(chapterWatched)
        call.respond(HttpStatusCode.Created)
        updateLocal.emit(CustomSSE.AddEvent(EventType.NEW_CHAPTER, chapterWatched.url))
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
        updateLocal.emit(CustomSSE.DeleteEvent(EventType.DELETE_CHAPTER, url))
    }
}

@Serializable
data class DeleteCount(val count: Int)

@Serializable
sealed class CustomSSE {
    abstract val eventType: EventType

    @Serializable
    data class DeleteEvent(
        override val eventType: EventType,
        val id: String,
    ) : CustomSSE()

    @Serializable
    data class AddEvent(
        override val eventType: EventType,
        val id: String,
    ) : CustomSSE()
}

enum class EventType {
    NEW_FAVORITE,
    DELETE_FAVORITE,
    NEW_CHAPTER,
    DELETE_CHAPTER,
    ADD_LIST,
    REMOVE_LIST,
    ADD_LIST_ITEM,
    REMOVE_LIST_ITEM,
}

fun getIP(): String? {
    var result: InetAddress? = null
    val interfaces = NetworkInterface.getNetworkInterfaces()
    while (interfaces.hasMoreElements()) {
        val addresses = interfaces.nextElement().inetAddresses
        while (addresses.hasMoreElements()) {
            val address = addresses.nextElement()
            if (!address.isLoopbackAddress) {
                if (address.isSiteLocalAddress) {
                    return address.hostAddress
                } else if (result == null) {
                    result = address
                }
            }
        }
    }
    return (result ?: InetAddress.getLocalHost()).hostAddress
}