package com.suda.yzune.wakeupschedule.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.suda.yzune.wakeupschedule.bean.TimeDetailBean
import com.suda.yzune.wakeupschedule.bean.TimeTableBean

@Dao
interface TimeTableDao {

    @Transaction
    suspend fun initTimeTable(timeTableBean: TimeTableBean) {
        val id = insertTimeTable(timeTableBean).toInt()
        val timeList = listOf(
                TimeDetailBean(1, "08:00", "08:45", id),
                TimeDetailBean(2, "08:50", "09:35", id),
                TimeDetailBean(3, "09:50", "10:35", id),
                TimeDetailBean(4, "10:40", "11:25", id),
                TimeDetailBean(5, "11:30", "12:15", id),
                TimeDetailBean(6, "13:00", "13:45", id),
                TimeDetailBean(7, "13:50", "14:35", id),
                TimeDetailBean(8, "14:45", "15:30", id),
                TimeDetailBean(9, "15:40", "16:25", id),
                TimeDetailBean(10, "16:35", "17:20", id),
                TimeDetailBean(11, "17:25", "18:10", id),
                TimeDetailBean(12, "18:30", "19:15", id),
                TimeDetailBean(13, "19:20", "20:05", id),
                TimeDetailBean(14, "20:10", "20:55", id),
                TimeDetailBean(15, "00:00", "00:00", id),
                TimeDetailBean(16, "00:00", "00:00", id),
                TimeDetailBean(17, "00:00", "00:00", id),
                TimeDetailBean(18, "00:00", "00:00", id),
                TimeDetailBean(19, "00:00", "00:00", id),
                TimeDetailBean(20, "00:00", "00:00", id),
                TimeDetailBean(21, "00:00", "00:00", id),
                TimeDetailBean(22, "00:00", "00:00", id),
                TimeDetailBean(23, "00:00", "00:00", id),
                TimeDetailBean(24, "00:00", "00:00", id),
                TimeDetailBean(25, "00:00", "00:00", id),
                TimeDetailBean(26, "00:00", "00:00", id),
                TimeDetailBean(27, "00:00", "00:00", id),
                TimeDetailBean(28, "00:00", "00:00", id),
                TimeDetailBean(29, "00:00", "00:00", id),
                TimeDetailBean(30, "00:00", "00:00", id)
        )
        insertTimeList(timeList)
    }

    @Insert
    suspend fun insertTimeList(list: List<TimeDetailBean>)

    @Insert
    suspend fun insertTimeTable(timeTableBean: TimeTableBean): Long

    @Query("select * from timetablebean")
    fun getTimeTableList(): LiveData<List<TimeTableBean>>

    @Query("select max(id) from timetablebean")
    suspend fun getMaxId(): Int

    @Query("select * from timetablebean where id = :id")
    suspend fun getTimeTable(id: Int): TimeTableBean?

    @Update
    suspend fun updateTimeTable(timeTableBean: TimeTableBean)

    @Delete
    suspend fun deleteTimeTable(timeTableBean: TimeTableBean)
}