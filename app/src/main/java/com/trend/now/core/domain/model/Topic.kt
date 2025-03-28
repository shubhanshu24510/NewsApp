package com.trend.now.core.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "topics")
data class Topic(
    @PrimaryKey(autoGenerate = false) val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)