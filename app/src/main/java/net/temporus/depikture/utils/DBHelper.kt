package net.temporus.depikture.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import net.temporus.depikture.objects.User

import org.jetbrains.anko.db.*

class DBHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private var instance: DBHelper? = null
        private val DATABASE_NAME = "Depicture"
        private val DATABASE_VERSION = 1

        @Synchronized
        fun getInstance(ctx: Context): DBHelper {
            if (instance == null) {
                instance = DBHelper(ctx.applicationContext)
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.createTable("User",true,
                "id" to SqlType.create("INTEGER PRIMARY KEY AUTOINCREMENT"),
                "name" to TEXT,
                "email" to TEXT,
                "password" to TEXT,
                "jwt" to TEXT)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.dropTable("User", true)
    }

    fun insertUser(user: User) {
        val db = this.writableDatabase
        db.insert("User",
                "name" to user.name,
                "email" to user.email,
                "password" to user.password,
                "jwt" to user.jwt)
        db.close()
    }

    fun updateUser(user: User) {
        val db = this.writableDatabase
        db.update("User",
                "name" to user.username,
                "email" to user.email,
                "password" to user.password,
                "jwt" to user.token)
                .whereArgs("_id = {userId}", "userId" to user.id.toString())
                .exec()
        db.close()
    }

    fun getUser(): User? {
        val db = this.readableDatabase
        val userlist = db.select("User").exec {
            parseList(classParser<User>())
        }
        db.close()
        return userlist.firstOrNull()
    }

}

val Context.database: DBHelper
    get() = DBHelper.getInstance(applicationContext)