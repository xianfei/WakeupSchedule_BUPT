package com.suda.yzune.wakeupschedule.schedule

import android.app.Application
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import biweekly.Biweekly
import biweekly.ICalVersion
import biweekly.ICalendar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.suda.yzune.wakeupschedule.App
import com.suda.yzune.wakeupschedule.AppDatabase
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.bean.*
import com.suda.yzune.wakeupschedule.schedule_import.Common
import com.suda.yzune.wakeupschedule.schedule_import.bean.SchoolInfo
import com.suda.yzune.wakeupschedule.utils.CourseUtils
import com.suda.yzune.wakeupschedule.utils.ICalUtils
import com.suda.yzune.wakeupschedule.utils.PreferenceKeys
import com.suda.yzune.wakeupschedule.utils.getPrefer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val dataBase = AppDatabase.getDatabase(application)
    private val courseDao = dataBase.courseDao()
    private val tableDao = dataBase.tableDao()
    private val widgetDao = dataBase.appWidgetDao()
    private val timeTableDao = dataBase.timeTableDao()
    private val timeDao = dataBase.timeDetailDao()

    lateinit var table: TableBean
    lateinit var timeList: List<TimeDetailBean>
    var selectedWeek = 1
    val marTop = application.resources.getDimensionPixelSize(R.dimen.weekItemMarTop)
    var itemHeight = 0
    var statusBarMargin = 0
    var alphaInt = 225
    val tableSelectList = arrayListOf<TableSelectBean>()
    val allCourseList = Array(7) { MutableLiveData<List<CourseBean>>() }
    val daysArray = arrayOf("日", "一", "二", "三", "四", "五", "六", "日")

    fun initTableSelectList(): LiveData<List<TableSelectBean>> {
        return tableDao.getTableSelectListLiveData()
    }

    fun getImportSchoolBean(): SchoolInfo {
        val json = getApplication<App>().getPrefer().getString(PreferenceKeys.IMPORT_SCHOOL, null)
                ?: return SchoolInfo("B", "北京邮电大学新教务", "http://jwgl.bupt.edu.cn/jsxsd", Common.TYPE_QZ_WITH_NODE)
        val gson = Gson()
        val res = gson.fromJson<SchoolInfo>(json, SchoolInfo::class.java)
        if (!res.type.isNullOrEmpty()) {
            return gson.fromJson<SchoolInfo>(json, SchoolInfo::class.java)
        }
        return SchoolInfo("B", "北京邮电大学新教务", "http://jwgl.bupt.edu.cn/jsxsd", Common.TYPE_QZ_WITH_NODE)
    }

    fun getMultiCourse(week: Int, day: Int, startNode: Int): List<CourseBean> {
        return allCourseList[day - 1].value!!.filter {
            it.inWeek(week) && it.startNode == startNode
        }
    }

    suspend fun getDefaultTable(): TableBean {
        return tableDao.getDefaultTable()
    }

    suspend fun getTimeList(timeTableId: Int): List<TimeDetailBean> {
        return timeDao.getTimeList(timeTableId)
    }

    suspend fun addBlankTable(tableName: String) {
        tableDao.insertTable(TableBean(id = 0, tableName = tableName))
    }

    suspend fun changeDefaultTable(id: Int) {
        tableDao.changeDefaultTable(table.id, id)
    }

    suspend fun getScheduleWidgetIds(): List<AppWidgetBean> {
        return widgetDao.getWidgetsByBaseType(0)
    }

    fun getRawCourseByDay(day: Int, tableId: Int): LiveData<List<CourseBean>> {
        return courseDao.getCourseByDayOfTableLiveData(day, tableId)
    }

    fun getShowCourseNumber(week: Int): LiveData<Int> {
        return if (table.showOtherWeekCourse) {
            courseDao.getShowCourseNumberWithOtherWeek(table.id, week)
        } else {
            courseDao.getShowCourseNumber(table.id, week)
        }
    }

    suspend fun deleteCourseBean(courseBean: CourseBean) {
        courseDao.deleteCourseDetail(CourseUtils.courseBean2DetailBean(courseBean))
    }

    suspend fun deleteCourseBaseBean(id: Int, tableId: Int) {
        courseDao.deleteCourseBaseBeanOfTable(id, tableId)
    }

    suspend fun updateFromOldVer(json: String) {
        val gson = Gson()
        val list = gson.fromJson<List<CourseOldBean>>(json, object : TypeToken<List<CourseOldBean>>() {
        }.type)
        val lastId = tableDao.getLastId()
        val tableId = if (lastId != null) {
            lastId + 1
        } else {
            1
        }
        oldBean2CourseBean(list, tableId)
    }

    private suspend fun oldBean2CourseBean(list: List<CourseOldBean>, tableId: Int) {
        val baseList = arrayListOf<CourseBaseBean>()
        val detailList = arrayListOf<CourseDetailBean>()
        var id = 0
        for (oldBean in list) {
            val flag = Common.findExistedCourseId(baseList, oldBean.name)
            if (flag == -1) {
                baseList.add(CourseBaseBean(id, oldBean.name, "", tableId))
                detailList.add(CourseDetailBean(
                        id = id, room = oldBean.room,
                        teacher = oldBean.teach, day = oldBean.day,
                        step = oldBean.step, startWeek = oldBean.startWeek, endWeek = oldBean.endWeek,
                        type = oldBean.isOdd, startNode = oldBean.start,
                        tableId = tableId
                ))
                id++
            } else {
                detailList.add(CourseDetailBean(
                        id = flag, room = oldBean.room,
                        teacher = oldBean.teach, day = oldBean.day,
                        step = oldBean.step, startWeek = oldBean.startWeek, endWeek = oldBean.endWeek,
                        type = oldBean.isOdd, startNode = oldBean.start,
                        tableId = tableId
                ))
            }
        }
        courseDao.insertCourses(baseList, detailList)
        getApplication<App>().getPrefer().edit {
            remove(PreferenceKeys.OLD_VERSION_COURSE)
        }
    }

    suspend fun exportData(currentDir: String): String {
        val myDir = if (currentDir.endsWith(File.separator)) {
            "${currentDir}WakeUp课程表/"
        } else {
            "$currentDir/WakeUp课程表/"
        }
        val dir = File(myDir)
        if (!dir.exists()) {
            dir.mkdir()
        }
        val gson = Gson()
        val strBuilder = StringBuilder()
        strBuilder.append(gson.toJson(timeTableDao.getTimeTable(table.timeTable)))
        strBuilder.append("\n${gson.toJson(timeList)}")
        strBuilder.append("\n${gson.toJson(table)}")
        strBuilder.append("\n${gson.toJson(courseDao.getCourseBaseBeanOfTable(table.id))}")
        strBuilder.append("\n${gson.toJson(courseDao.getDetailOfTable(table.id))}")
        val tableName = if (table.tableName == "") {
            "我的课表"
        } else {
            table.tableName
        }
        val file = File(myDir, "$tableName${Calendar.getInstance().timeInMillis}.wakeup_schedule")
        file.writeText(strBuilder.toString())
        return file.path
    }

    suspend fun exportICS(currentDir: String): String {
        val myDir = if (currentDir.endsWith(File.separator)) {
            "${currentDir}WakeUp课程表/"
        } else {
            "$currentDir/WakeUp课程表/"
        }
        val dir = File(myDir)
        if (!dir.exists()) {
            dir.mkdir()
        }
        //val week = CourseUtils.countWeekForExport(table.startDate, table.sundayFirst)
        withContext(Dispatchers.Default) {

        }
        val ical = ICalendar()
        withContext(Dispatchers.Default) {
            ical.setProductId("-//YZune//WakeUpSchedule//EN")
//        calendar.properties.add(ProdId("-//WakeUpSchedule //iCal4j 2.0//EN"))
//        calendar.properties.add(Version.VERSION_2_0)
//        calendar.properties.add(CalScale.GREGORIAN)
            val startTimeMap = ICalUtils.getClassTime(timeList, true)
            val endTimeMap = ICalUtils.getClassTime(timeList, false)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            val date = sdf.parse(table.startDate)
            allCourseList.forEach {
                it.value?.forEach { course ->
                    try {
                        ICalUtils.getClassEvents(ical, startTimeMap, endTimeMap, table.maxWeek, course, date)
                    } catch (ignored: Exception) {

                    }
                }
            }
        }
        val warnings = ical.validate(ICalVersion.V2_0)
        Log.d("日历", warnings.toString())
        val tableName = if (table.tableName == "") {
            "我的课表"
        } else {
            table.tableName
        }
        val file = File(myDir, "日历-$tableName.ics")
        withContext(Dispatchers.IO) {
            Biweekly.write(ical).go(file)
        }
        return file.path
    }
}