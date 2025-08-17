package com.jambus.wikihelper.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jambus.wikihelper.data.local.converters.DateConverter
import com.jambus.wikihelper.data.local.dao.ChatDao
import com.jambus.wikihelper.data.local.entity.ChatMessageEntity
import com.jambus.wikihelper.data.local.entity.ConversationEntity

@Database(
    entities = [ChatMessageEntity::class, ConversationEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class WikiHelperDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        const val DATABASE_NAME = "wiki_helper_db"
    }
}