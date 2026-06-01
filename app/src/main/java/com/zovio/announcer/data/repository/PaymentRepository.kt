package com.zovio.announcer.data.repository

import com.zovio.announcer.data.db.PaymentDao
import com.zovio.announcer.data.db.PaymentEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class PaymentRepository(private val dao: PaymentDao) {

    private fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun startOfWeek(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek) // Usually Sunday or Monday depending on locale
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun startOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    suspend fun insertPayment(payment: PaymentEntity): Long {
        return dao.insertPayment(payment)
    }

    fun getAllPayments(): Flow<List<PaymentEntity>> {
        return dao.getAllPayments()
    }

    fun getTodaysPayments(): Flow<List<PaymentEntity>> {
        return dao.getTodaysPayments(startOfToday())
    }

    fun getTodayTotal(): Flow<Double> {
        return dao.getTodayTotal(startOfToday())
    }

    fun getWeekTotal(): Flow<Double> {
        return dao.getWeekTotal(startOfWeek())
    }

    fun getMonthTotal(): Flow<Double> {
        return dao.getMonthTotal(startOfMonth())
    }

    fun getTotalCount(): Flow<Int> {
        return dao.getTotalCount()
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}
