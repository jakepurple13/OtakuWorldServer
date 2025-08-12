package com.programmersbox.otakuworld.databases

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ListSchema(database: Database) : GenericSchema() {

    object CustomListItemModel : Table() {
        val uuid = varchar("uuid", 200)
        val name = varchar("name", 200)
        val time = long("time")
        val useBiometric = bool("useBiometric")

        override val primaryKey: PrimaryKey = PrimaryKey(uuid)
    }

    object CustomListModel : Table() {
        val uniqueId = varchar("uniqueId", 200)
        val uuid = varchar("uuid", 200)
        val title = varchar("title", 200)
        val description = varchar("description", 1000)
        val url = varchar("url", 200)
        val imageUrl = varchar("imageUrl", 300)
        val sources = varchar("sources", 200)

        override val primaryKey: PrimaryKey = PrimaryKey(uniqueId)
    }

    suspend fun createList(listItem: CustomListItem) = dbQuery {
        CustomListItemModel.upsert {
            it[uuid] = listItem.uuid
            it[name] = listItem.name
            it[time] = listItem.time
            it[useBiometric] = listItem.useBiometric
        }
    }

    suspend fun addItem(list: CustomListInfo) = dbQuery {
        CustomListModel.upsert {
            it[uniqueId] = list.uniqueId
            it[uuid] = list.uuid
            it[title] = list.title
            it[description] = list.description
            it[url] = list.url
        }
    }

    suspend fun getList(uniqueId: String) = dbQuery {
        CustomListModel.selectAll().where { CustomListModel.uniqueId eq uniqueId }.map {
            CustomListInfo(
                uniqueId = it[CustomListModel.uniqueId],
                uuid = it[CustomListModel.uuid],
                title = it[CustomListModel.title],
                description = it[CustomListModel.description],
                url = it[CustomListModel.url],
                imageUrl = it[CustomListModel.imageUrl],
                source = it[CustomListModel.sources],
            )
        }
    }

    suspend fun deleteList(uniqueId: String) = dbQuery {
        CustomListModel.deleteWhere { CustomListModel.uniqueId eq uniqueId }
    }

    suspend fun deleteFullList(uniqueId: String) = dbQuery {
        val list = CustomListItemModel.deleteWhere { CustomListItemModel.uuid eq uniqueId }
        val items = CustomListModel.deleteWhere { CustomListModel.uniqueId eq uniqueId }

        list + items
    }

    suspend fun getAllLists() = dbQuery {
        CustomListItemModel
            .selectAll()
            .map {
                CustomList(
                    item = CustomListItem(
                        uuid = it[CustomListItemModel.uuid],
                        name = it[CustomListItemModel.name],
                        time = it[CustomListItemModel.time],
                        useBiometric = it[CustomListItemModel.useBiometric]
                    ),
                    CustomListModel
                        .selectAll()
                        .where { CustomListModel.uuid eq it[CustomListItemModel.uuid] }
                        .map {
                            CustomListInfo(
                                uniqueId = it[CustomListModel.uniqueId],
                                uuid = it[CustomListModel.uuid],
                                title = it[CustomListModel.title],
                                description = it[CustomListModel.description],
                                url = it[CustomListModel.url],
                                imageUrl = it[CustomListModel.imageUrl],
                                source = it[CustomListModel.sources]
                            )
                        }
                )
            }

        /*val items = CustomListModel
            .selectAll()
            .map {
                CustomListInfo(
                    uniqueId = it[CustomListModel.uniqueId],
                    uuid = it[CustomListModel.uuid],
                    title = it[CustomListModel.title],
                    description = it[CustomListModel.description],
                    url = it[CustomListModel.url],
                    imageUrl = it[CustomListModel.imageUrl],
                    source = it[CustomListModel.sources]
                )
            }

        list.map { CustomList(it, items.filter { i -> i.uuid == it.uuid }) }*/
    }
}

@Serializable
data class CustomList(
    val item: CustomListItem,
    val list: List<CustomListInfo>,
)

@OptIn(ExperimentalTime::class)
@Serializable
data class CustomListItem(
    //@PrimaryKey
    val uuid: String,
    val name: String,
    val time: Long = Clock.System.now().toEpochMilliseconds(),
    val useBiometric: Boolean = false,
)

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class CustomListInfo(
    //@PrimaryKey
    val uniqueId: String = Uuid.random().toString(),
    val uuid: String,
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String,
    val source: String,
)
