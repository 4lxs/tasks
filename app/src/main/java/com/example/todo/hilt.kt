package com.example.todo

import android.content.Context
import androidx.room.Room
import com.example.todo.data.db.AppDatabase
import com.example.todo.data.db.EventDao
import com.example.todo.data.db.CalendarRepositoryImpl
import com.example.todo.data.db.FilSorterDao
import com.example.todo.data.db.FilSorterRepositoryImpl
import com.example.todo.data.db.TagRepositoryImpl
import com.example.todo.data.db.TasksDao
import com.example.todo.domain.CalendarRepository
import com.example.todo.domain.FilSorterRepository
import com.example.todo.domain.TagRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {
    @Provides
    fun provideTodoItemDao(appDb: AppDatabase): TasksDao = appDb.tasksDao()

    @Provides
    fun provideEventDao(appDb: AppDatabase): EventDao = appDb.eventDao()

    @Provides
    fun provideFilSorterDao(appDb: AppDatabase): FilSorterDao = appDb.filSorterDao()

    @Provides
    @Singleton
    fun provideAppDb(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context = context, AppDatabase::class.java, "app_database")
            .fallbackToDestructiveMigration(true).build()
    }
}

@InstallIn(SingletonComponent::class)
@Module
abstract class RepositoryModule {

    @Binds
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository

    @Binds
    abstract fun bindCalRepository(impl: CalendarRepositoryImpl): CalendarRepository

    @Binds
    abstract fun bindFilSorterRepository(impl: FilSorterRepositoryImpl): FilSorterRepository
}

