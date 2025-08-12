package com.programmersbox.otakuworld

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ExposedUser(val name: String, val age: Int)

class UserService(database: Database) {
    object Users : Table() {
        val id = integer("id").autoIncrement()
        val name = varchar("name", length = 50)
        val age = integer("age")

        override val primaryKey = PrimaryKey(id)
    }

    object DbModels : Table() {
        val url = varchar("url", length = 200)
        val title = varchar("title", length = 200)
        val description = varchar("description", length = 1000)
        val imageUrl = varchar("imageUrl", length = 300)
        val sources = varchar("sources", length = 200)
        val numChapters = integer("numChapters")
        val shouldCheckForUpdate = bool("shouldCheckForUpdate")
        val type = varchar("type", length = 50)

        override val primaryKey: PrimaryKey = PrimaryKey(url)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Users, DbModels)
        }
    }

    suspend fun create(user: ExposedUser): Int = dbQuery {
        Users.insert {
            it[name] = user.name
            it[age] = user.age
        }[Users.id]
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

    suspend fun read(id: Int): ExposedUser? {
        return dbQuery {
            Users.selectAll()
                .where { Users.id eq id }
                .map { ExposedUser(it[Users.name], it[Users.age]) }
                .singleOrNull()
        }
    }

    suspend fun update(id: Int, user: ExposedUser) {
        dbQuery {
            Users.update({ Users.id eq id }) {
                it[name] = user.name
                it[age] = user.age
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            Users.deleteWhere { Users.id.eq(id) }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
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