package com.mandg.funny.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class GameDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "game.db"
        private const val DATABASE_VERSION = 2

        // Table game_level
        const val TABLE_LEVEL = "game_level"
        const val COL_LEVEL_ID = "_id"
        const val COL_LEVEL_NUM = "level"
        const val COL_LEVEL_SCORE = "score"
        const val COL_LEVEL_STARS = "stars"

        // Table game_icon
        const val TABLE_ICON = "game_icon"
        const val COL_ICON_ID = "_id"
        const val COL_ICON_PK_NAME = "pk_name"
        const val COL_ICON_ACTIVITY_NAME = "activity_name"
        const val COL_ICON_PATH = "path"
        const val COL_ICON_ICON_PATH = "icon_path"
        const val COL_ICON_TYPE = "type"
        const val COL_ICON_BLOCK_TYPE = "block_type"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createLevelTable = """
            CREATE TABLE $TABLE_LEVEL (
                $COL_LEVEL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_LEVEL_NUM INTEGER UNIQUE,
                $COL_LEVEL_SCORE INTEGER,
                $COL_LEVEL_STARS INTEGER
            )
        """.trimIndent()

        val createIconTable = """
            CREATE TABLE $TABLE_ICON (
                $COL_ICON_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ICON_PK_NAME TEXT,
                $COL_ICON_ACTIVITY_NAME TEXT,
                $COL_ICON_ICON_PATH TEXT,
                $COL_ICON_PATH TEXT,
                $COL_ICON_TYPE INTEGER,
                $COL_ICON_BLOCK_TYPE INTEGER
            )
        """.trimIndent()

        db.execSQL(createLevelTable)
        db.execSQL(createIconTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Upgrade logic if needed
    }

    // Methods for Level Progress
    fun getLevelStars(levelNum: Int): Int {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_LEVEL,
            arrayOf(COL_LEVEL_STARS),
            "$COL_LEVEL_NUM = ?",
            arrayOf(levelNum.toString()),
            null, null, null
        )
        var stars = 0
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                stars = cursor.getInt(cursor.getColumnIndexOrThrow(COL_LEVEL_STARS))
            }
            cursor.close()
        }
        return stars
    }

    fun getLevelHighScore(levelNum: Int): Int {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_LEVEL,
            arrayOf(COL_LEVEL_SCORE),
            "$COL_LEVEL_NUM = ?",
            arrayOf(levelNum.toString()),
            null, null, null
        )
        var score = 0
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                score = cursor.getInt(cursor.getColumnIndexOrThrow(COL_LEVEL_SCORE))
            }
            cursor.close()
        }
        return score
    }

    fun saveLevelProgress(levelNum: Int, score: Int, stars: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_LEVEL_NUM, levelNum)
            put(COL_LEVEL_SCORE, score)
            put(COL_LEVEL_STARS, stars)
        }
        db.insertWithOnConflict(
            TABLE_LEVEL,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }
}
