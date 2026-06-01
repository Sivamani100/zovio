package com.zovio.announcer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val senderName: String?,
    val appSource: String,
    val rawNotificationText: String,
    val timestamp: Long = System.currentTimeMillis()
)
