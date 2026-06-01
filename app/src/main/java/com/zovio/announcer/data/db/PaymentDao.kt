package com.zovio.announcer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Query("SELECT * FROM payments ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    fun getTodaysPayments(startOfDay: Long): Flow<List<PaymentEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE timestamp >= :startOfDay")
    fun getTodayTotal(startOfDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE timestamp >= :startOfWeek")
    fun getWeekTotal(startOfWeek: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE timestamp >= :startOfMonth")
    fun getMonthTotal(startOfMonth: Long): Flow<Double>

    @Query("SELECT COUNT(*) FROM payments")
    fun getTotalCount(): Flow<Int>

    @Query("DELETE FROM payments")
    suspend fun deleteAll()
}
