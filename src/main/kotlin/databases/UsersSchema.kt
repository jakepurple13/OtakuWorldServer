package com.programmersbox.otakuworld.databases

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert

class UserService : GenericSchema() {
    object DbModels : Table() {
        val url = text("url")
        val title = text("title")
        val description = text("description")
        val imageUrl = text("imageUrl")
        val sources = varchar("sources", length = 200)
        val numChapters = integer("numChapters")
        val shouldCheckForUpdate = bool("shouldCheckForUpdate")
        val type = varchar("type", length = 50)

        override val primaryKey: PrimaryKey = PrimaryKey(url)
    }

    object ChapterWatchedModels : Table() {
        val url = text("url")
        val name = text("name")
        val favoriteUrl = text("favoriteUrl")

        override val primaryKey: PrimaryKey = PrimaryKey(url)
    }

    suspend fun create(model: DbModel) = dbQuery {
        DbModels.upsert {
            it[url] = model.url
            it[title] = model.title
            it[description] = model.description
            it[imageUrl] = model.imageUrl
            it[sources] = model.source
            it[numChapters] = model.numChapters
            it[shouldCheckForUpdate] = model.shouldCheckForUpdate
            it[type] = model.type
        }[DbModels.url]
    }

    suspend fun getModels(type: String): List<DbModel> = dbQuery {
        DbModels
            .selectAll()
            .where { DbModels.type eq type }
            .map {
                DbModel(
                    it[DbModels.title],
                    it[DbModels.description],
                    it[DbModels.url],
                    it[DbModels.imageUrl],
                    it[DbModels.sources],
                    it[DbModels.numChapters],
                    it[DbModels.shouldCheckForUpdate],
                    it[DbModels.type]
                )
            }
    }

    suspend fun getFavorite(url: String) = dbQuery {
        DbModels
            .selectAll()
            .where { DbModels.url eq url }
            .map {
                DbModel(
                    it[DbModels.title],
                    it[DbModels.description],
                    it[DbModels.url],
                    it[DbModels.imageUrl],
                    it[DbModels.sources],
                    it[DbModels.numChapters],
                    it[DbModels.shouldCheckForUpdate],
                    it[DbModels.type]
                )
            }
    }

    suspend fun deleteModel(url: String) = dbQuery {
        DbModels.deleteWhere { DbModels.url eq url }
    }

    /*suspend fun read(id: Int): ExposedUser? {
        return dbQuery {
            Users.selectAll()
                .where { Users.id eq id }
                .map { ExposedUser(it[Users.name], it[Users.age]) }
                .singleOrNull()
        }
    }*/

    suspend fun addChapterWatched(chapterWatched: ChapterWatched) = dbQuery {
        ChapterWatchedModels.upsert {
            it[url] = chapterWatched.url
            it[name] = chapterWatched.name
            it[favoriteUrl] = chapterWatched.favoriteUrl
        }[ChapterWatchedModels.url]
    }

    suspend fun getChapterWatched(favoriteUrl: String): List<ChapterWatched> = dbQuery {
        ChapterWatchedModels
            .selectAll()
            .where { ChapterWatchedModels.favoriteUrl eq favoriteUrl }
            .map {
                ChapterWatched(
                    it[ChapterWatchedModels.url],
                    it[ChapterWatchedModels.name],
                    it[ChapterWatchedModels.favoriteUrl]
                )
            }
    }

    suspend fun getChapterWatchedUrl(url: String) = dbQuery {
        ChapterWatchedModels
            .selectAll()
            .where { ChapterWatchedModels.url eq url }
            .map {
                ChapterWatched(
                    it[ChapterWatchedModels.url],
                    it[ChapterWatchedModels.name],
                    it[ChapterWatchedModels.favoriteUrl]
                )
            }
    }

    suspend fun removeChapterWatched(url: String) = dbQuery {
        ChapterWatchedModels.deleteWhere { ChapterWatchedModels.url eq url }
    }
}

@Serializable
data class DbModel(
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String,
    val source: String,
    val numChapters: Int,
    val shouldCheckForUpdate: Boolean,
    val type: String,
)

@Serializable
data class ChapterWatched(
    val url: String,
    val name: String,
    val favoriteUrl: String,
)