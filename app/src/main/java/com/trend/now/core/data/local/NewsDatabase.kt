package com.trend.now.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.trend.now.core.domain.model.NewsCache
import com.trend.now.core.domain.model.Topic

@Database(entities = [Topic::class, NewsCache::class], version = 2)
abstract class NewsDatabase : RoomDatabase() {
    abstract fun topicDao(): TopicDao
    abstract fun newsCacheDao(): NewsCacheDao
}