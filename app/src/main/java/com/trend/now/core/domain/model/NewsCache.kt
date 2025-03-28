package com.trend.now.core.domain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "news_cache",
    indices = [Index(value = ["url"], unique = true)]
)
data class NewsCache(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val parentUrl: String,
    val createdAt: Long
)