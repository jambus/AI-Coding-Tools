package com.jambus.wikihelper.di

import android.content.Context
import androidx.room.Room
import com.jambus.wikihelper.data.local.database.WikiHelperDatabase
import com.jambus.wikihelper.data.repository.ChatRepository
import com.jambus.wikihelper.data.repository.DifyRepository
import com.jambus.wikihelper.data.security.SecurityManager
import com.jambus.wikihelper.data.local.dao.ChatDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WikiHelperDatabase {
        return Room.databaseBuilder(
            context,
            WikiHelperDatabase::class.java,
            WikiHelperDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideChatDao(database: WikiHelperDatabase) = database.chatDao()

    @Provides
    @Singleton
    fun provideSecurityManager(@ApplicationContext context: Context) = SecurityManager(context)

    @Provides
    @Singleton
    fun provideChatRepository(
        chatDao: ChatDao
    ) = ChatRepository(chatDao)
}