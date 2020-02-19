package com.suda.yzune.wakeupschedule.schedule

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.UpdateFragment
import com.suda.yzune.wakeupschedule.apply_info.ApplyInfoActivity
import com.suda.yzune.wakeupschedule.base_view.BaseActivity
import com.suda.yzune.wakeupschedule.bean.TableBean
import com.suda.yzune.wakeupschedule.bean.TableSelectBean
import com.suda.yzune.wakeupschedule.bean.UpdateInfoBean
import com.suda.yzune.wakeupschedule.course_add.AddCourseActivity
import com.suda.yzune.wakeupschedule.intro.AboutActivity
import com.suda.yzune.wakeupschedule.intro.IntroYoungActivity
import com.suda.yzune.wakeupschedule.schedule_import.LoginWebActivity
import com.suda.yzune.wakeupschedule.schedule_manage.ScheduleManageActivity
import com.suda.yzune.wakeupschedule.schedule_settings.ScheduleSettingsActivity
import com.suda.yzune.wakeupschedule.settings.SettingsActivity
import com.suda.yzune.wakeupschedule.suda_life.SudaLifeActivity
import com.suda.yzune.wakeupschedule.utils.*
import com.suda.yzune.wakeupschedule.utils.CourseUtils.countWeek
import com.suda.yzune.wakeupschedule.utils.UpdateUtils.getVersionCode
import es.dmoral.toasty.Toasty
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import splitties.activities.start
import splitties.dimensions.dip
import java.text.ParseException
import kotlin.math.roundToInt

class ScheduleActivity : BaseActivity() {

    private val viewModel by viewModels<ScheduleViewModel>()
    private var mAdapter: SchedulePagerAdapter? = null

    private lateinit var scheduleViewPager: ViewPager
    private lateinit var bgImageView: AppCompatImageView
    private lateinit var scheduleConstraintLayout: ConstraintLayout
    private lateinit var navImageButton: AppCompatTextView
    private lateinit var shareImageButton: AppCompatTextView
    private lateinit var addImageButton: AppCompatTextView
    private lateinit var importImageButton: AppCompatTextView
    private lateinit var moreImageButton: AppCompatTextView
    private lateinit var tableNameRecyclerView: RecyclerView
    private lateinit var dateTextView: AppCompatTextView
    private lateinit var weekTextView: AppCompatTextView
    private lateinit var weekdayTextView: AppCompatTextView
    private lateinit var navigationView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private val preLoad by lazy(LazyThreadSafetyMode.NONE) {
        getPrefer().getBoolean(PreferenceKeys.SCHEDULE_PRE_LOAD, true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (getPrefer().getBoolean(PreferenceKeys.HIDE_NAV_BAR, true) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }
        setContentView(ScheduleActivityUI(this).root)

        val json = getPrefer().getString(PreferenceKeys.OLD_VERSION_COURSE, "")
        if (!json.isNullOrEmpty()) {
            launch {
                try {
                    viewModel.updateFromOldVer(json)
                    Toasty.success(applicationContext, "升级成功~").show()
                } catch (e: Exception) {
                    Toasty.error(applicationContext, "出现异常>_<\n${e.message}").show()
                }
            }
        }

        scheduleViewPager = findViewById(R.id.anko_vp_schedule)
        bgImageView = findViewById(R.id.anko_iv_bg)
        scheduleConstraintLayout = findViewById(R.id.anko_cl_schedule)
        navImageButton = findViewById(R.id.anko_ib_nav)
        shareImageButton = findViewById(R.id.anko_ib_share)
        addImageButton = findViewById(R.id.anko_ib_add)
        importImageButton = findViewById(R.id.anko_ib_import)
        moreImageButton = findViewById(R.id.anko_ib_more)
        tableNameRecyclerView = findViewById(R.id.anko_rv_table_name)
        dateTextView = findViewById(R.id.anko_tv_date)
        weekTextView = findViewById(R.id.anko_tv_week)
        weekdayTextView = findViewById(R.id.anko_tv_weekday)
        navigationView = findViewById(R.id.anko_nv)
        drawerLayout = findViewById(R.id.anko_drawer_layout)

        initView()
        initNavView()

//        val openTimes = getPrefer().getInt(PreferenceKeys.OPEN_TIMES, 0)
//        if (openTimes < 10) {
//            getPrefer().edit {
//                putInt(PreferenceKeys.OPEN_TIMES, openTimes + 1)
//            }
//        } else if (openTimes == 10) {
//            val dialog = DonateFragment.newInstance()
//            dialog.isCancelable = false
//            dialog.show(supportFragmentManager, "donateDialog")
//            getPrefer().edit {
//                putInt(PreferenceKeys.OPEN_TIMES, openTimes + 1)
//            }
//        }

        if (!getPrefer().getBoolean(PreferenceKeys.HAS_COUNT, false)) {
            MyRetrofitUtils.instance.addCount(applicationContext)
        }

        if (getPrefer().getBoolean(PreferenceKeys.CHECK_UPDATE, false)) {
            MyRetrofitUtils.instance.getService().getUpdateInfo().enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {}

                override fun onResponse(call: Call<ResponseBody>?, response: Response<ResponseBody>?) {
                    if (response!!.body() != null) {
                        val gson = Gson()
                        try {
                            val updateInfo = gson.fromJson<UpdateInfoBean>(response.body()!!.string(), UpdateInfoBean::class.java)
                            if (updateInfo.id > getVersionCode(this@ScheduleActivity.applicationContext)) {
                                UpdateFragment.newInstance(updateInfo).show(supportFragmentManager, "updateDialog")
                            }
                        } catch (e: Exception) {

                        }
                    }
                }
            })
        }

        if (!getPrefer().getBoolean(PreferenceKeys.HAS_INTRO, false)) {
            initIntro()
        }

        viewModel.initTableSelectList().observe(this, Observer {
            if (it == null) return@Observer
            viewModel.tableSelectList.clear()
            viewModel.tableSelectList.addAll(it)
            if (tableNameRecyclerView.adapter == null) {
                initTableMenu(viewModel.tableSelectList)
            } else {
                tableNameRecyclerView.adapter?.notifyDataSetChanged()
            }
        })

        //DonateFragment.newInstance().show(supportFragmentManager, "AfterImportTipFragment")
    }

    private fun initTheme() {
        if (viewModel.table.background != "") {
            val x = (ViewUtils.getRealSize(this).x * 0.5).toInt()
            val y = (ViewUtils.getRealSize(this).y * 0.5).toInt()
            Glide.with(this)
                    .load(viewModel.table.background)
                    .override(x, y)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.main_background_2020_1)
                    .into(bgImageView)
            Glide.with(this)
                    .load(viewModel.table.background)
                    .override((x * 0.8).toInt(), (y * 0.8).toInt())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.main_background_2020_1)
                    .into(navigationView.getHeaderView(0).findViewById(R.id.iv_header))
        } else {
            val x = (ViewUtils.getRealSize(this).x * 0.5).toInt()
            val y = (ViewUtils.getRealSize(this).y * 0.5).toInt()
            Glide.with(this)
                    .load(R.drawable.main_background_2020_1)
                    .override(x, y)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(bgImageView)
            Glide.with(this)
                    .load(R.drawable.main_background_2020_1)
                    .override((x * 0.8).toInt(), (y * 0.8).toInt())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(navigationView.getHeaderView(0).findViewById(R.id.iv_header))
        }

        for (i in 0 until scheduleConstraintLayout.childCount) {
            val view = scheduleConstraintLayout.getChildAt(i)
            when (view) {
                is AppCompatTextView -> view.setTextColor(viewModel.table.textColor)
                is AppCompatImageButton -> view.setColorFilter(viewModel.table.textColor)
            }
        }

        if (ViewUtils.judgeColorIsLight(viewModel.table.textColor)) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            }
        }

        viewModel.itemHeight = dip(viewModel.table.itemHeight)
    }

    private fun initTableMenu(data: MutableList<TableSelectBean>) {
        tableNameRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val adapter = TableNameAdapter(R.layout.item_table_select_main, data)
        adapter.addHeaderView(FrameLayout(this).apply {
            this.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dip(24))
        })
        adapter.addFooterView(initFooterView())
        adapter.addChildClickViewIds(R.id.menu_setting, R.id.menu_export)
        adapter.setOnItemChildClickListener { _, view, _ ->
            when (view.id) {
                R.id.menu_setting -> {
                    startActivityForResult(Intent(this,
                            ScheduleSettingsActivity::class.java).apply {
                        putExtra("tableData", viewModel.table)
                    }, 16)
                }
                R.id.menu_export -> {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                    } else {
                        ExportSettingsFragment().show(supportFragmentManager, "exportSettingsFragment")
                    }
                }
            }
        }
        adapter.setOnItemClickListener { _, _, position ->
            if (position < data.size) {
                if (data[position].id != viewModel.table.id) {
                    launch {
                        viewModel.changeDefaultTable(data[position].id)
                        initView()
                        val list = viewModel.getScheduleWidgetIds()
                        val table = viewModel.getDefaultTable()
                        list.forEach {
                            when (it.detailType) {
                                // 0 -> AppWidgetUtils.refreshScheduleWidget(applicationContext, appWidgetManager, it.id, table)
                                1 -> AppWidgetUtils.refreshTodayWidget(applicationContext, appWidgetManager, it.id, table)
                            }
                        }
                    }
                }
            }
        }
        tableNameRecyclerView.adapter = adapter
    }

    private fun initFooterView(): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_table_add_main, tableNameRecyclerView, false)
        val tableAdd = view.findViewById<AppCompatTextView>(R.id.nav_table_add)
        tableAdd.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.setting_schedule_name)
                    .setView(R.layout.dialog_edit_text)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.sure, null)
                    .create()
            dialog.show()
            val inputLayout = dialog.findViewById<TextInputLayout>(R.id.text_input_layout)
            val editText = dialog.findViewById<TextInputEditText>(R.id.edit_text)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = editText?.text
                if (value.isNullOrBlank()) {
                    inputLayout?.error = "名称不能为空哦>_<"
                } else {
                    launch {
                        try {
                            viewModel.addBlankTable(editText.text.toString())
                            Toasty.success(this@ScheduleActivity, "新建成功~").show()
                        } catch (e: Exception) {
                            Toasty.error(this@ScheduleActivity, "操作失败>_<").show()
                        }
                        dialog.dismiss()
                    }
                }
            }
        }
        val tableManage = view.findViewById<AppCompatTextView>(R.id.nav_table_manage)
        tableManage.setOnClickListener {
            startActivityForResult(
                    Intent(this, ScheduleManageActivity::class.java), 16)
        }
        return view
    }

    fun initIntro() {
    }

    override fun onStart() {
        super.onStart()
        dateTextView.text = CourseUtils.getTodayDate()
    }

    private fun initNavView() {
        navigationView.menu.findItem(R.id.nav_suda).isVisible = getPrefer().getBoolean(PreferenceKeys.SHOW_SUDA_LIFE, false)
        navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_setting -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    drawerLayout.postDelayed({
                        startActivityForResult(Intent(this, SettingsActivity::class.java), 31)
                    }, 360)
                    return@setNavigationItemSelectedListener true
                }
                R.id.nav_explore -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    drawerLayout.postDelayed({
                        start<ApplyInfoActivity>()
                    }, 360)
                    return@setNavigationItemSelectedListener true
                }
                R.id.nav_feedback -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    drawerLayout.postDelayed({
                        Utils.openUrl(this, "https://support.qq.com/product/97617")
                    }, 360)
                    return@setNavigationItemSelectedListener true
                }
                R.id.nav_about -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    drawerLayout.postDelayed({
                        start<AboutActivity>()
                    }, 360)
                    return@setNavigationItemSelectedListener true
                }
                R.id.nav_young -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    drawerLayout.postDelayed({
                        start<IntroYoungActivity>()
                    }, 360)
                    return@setNavigationItemSelectedListener true
                }
                R.id.nav_empty_room -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    drawerLayout.postDelayed({
                        start<SudaLifeActivity> {
                            putExtra("type", "空教室")
                        }
                    }, 360)
                    return@setNavigationItemSelectedListener true
                }
                R.id.nav_bathroom -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    drawerLayout.postDelayed({
                        start<SudaLifeActivity> {
                            putExtra("type", "澡堂")
                        }
                    }, 360)
                    return@setNavigationItemSelectedListener true
                }
                else -> {
                    Toasty.info(this.applicationContext, "敬请期待").show()
                    return@setNavigationItemSelectedListener true
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ExportSettingsFragment().show(supportFragmentManager, "exportSettingsFragment")
                } else {
                    Toasty.error(applicationContext, "你取消了授权>_<无法导出", Toast.LENGTH_LONG).show()
                }
            }
            2 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    start<LoginWebActivity> {
                        putExtra("import_type", "file")
                    }
                } else {
                    Toasty.error(applicationContext, "你取消了授权>_<无法从文件导入", Toast.LENGTH_LONG).show()
                }
            }
            3 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    start<LoginWebActivity> {
                        putExtra("import_type", "excel")
                        putExtra("tableId", viewModel.table.id)
                    }
                } else {
                    Toasty.error(applicationContext, "你取消了授权>_<无法从文件导入", Toast.LENGTH_LONG).show()
                }
            }
            4 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    start<LoginWebActivity> {
                        putExtra("import_type", "html")
                        putExtra("tableId", viewModel.table.id)
                    }
                } else {
                    Toasty.error(applicationContext, "你取消了授权>_<无法从文件导入", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun initViewPage(maxWeek: Int, table: TableBean) {
        if (mAdapter == null) {
            mAdapter = SchedulePagerAdapter(maxWeek, preLoad, supportFragmentManager)
            scheduleViewPager.adapter = mAdapter
            scheduleViewPager.offscreenPageLimit = 1
        }
        mAdapter!!.maxWeek = maxWeek
        mAdapter!!.notifyDataSetChanged()
        if (CourseUtils.countWeek(table.startDate, table.sundayFirst) > 0) {
            scheduleViewPager.currentItem = CourseUtils.countWeek(table.startDate, table.sundayFirst) - 1
        } else {
            scheduleViewPager.currentItem = 0
        }
    }

    private fun initEvent() {
        addImageButton.setOnClickListener {
            start<AddCourseActivity> {
                putExtra("tableId", viewModel.table.id)
                putExtra("maxWeek", viewModel.table.maxWeek)
                putExtra("nodes", viewModel.table.nodes)
                putExtra("id", -1)
            }
        }

        moreImageButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        navImageButton.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        shareImageButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            } else {
                ExportSettingsFragment().show(supportFragmentManager, "share")
            }
        }

        importImageButton.setOnClickListener {
            ImportChooseFragment().show(supportFragmentManager, "importDialog")
        }

        weekdayTextView.setOnClickListener {
            weekdayTextView.text = CourseUtils.getWeekday()
            val currentWeek = countWeek(viewModel.table.startDate, viewModel.table.sundayFirst)
            if (currentWeek > 0) {
                scheduleViewPager.currentItem = currentWeek - 1
            } else {
                scheduleViewPager.currentItem = 0
            }
        }

        scheduleViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageSelected(position: Int) {
                viewModel.selectedWeek = position + 1
                val currentWeek = countWeek(viewModel.table.startDate, viewModel.table.sundayFirst)
                try {
                    if (currentWeek > 0) {
                        if (viewModel.selectedWeek == currentWeek) {
                            weekTextView.text = "第${viewModel.selectedWeek}周"
                            weekdayTextView.text = CourseUtils.getWeekday()
                        } else {
                            weekTextView.text = "第${viewModel.selectedWeek}周"
                            weekdayTextView.text = "非本周"
                        }
                    } else {
                        weekTextView.text = "还没有开学哦"
                        weekdayTextView.text = CourseUtils.getWeekday()
                    }
                } catch (e: ParseException) {
                    e.printStackTrace()
                }

            }

            override fun onPageScrolled(a: Int, b: Float, c: Int) {

            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })
    }

    private fun initView() {
        launch {
            viewModel.table = viewModel.getDefaultTable()

            val currentWeek = countWeek(viewModel.table.startDate, viewModel.table.sundayFirst)

            if (currentWeek > 0) {
                weekTextView.text = "第${currentWeek}周"
            } else {
                weekTextView.text = "还没有开学哦"
            }

            weekdayTextView.text = CourseUtils.getWeekday()

            initTheme()

            viewModel.alphaInt = (255 * (viewModel.table.itemAlpha.toFloat() / 100)).roundToInt()

            initViewPage(viewModel.table.maxWeek, viewModel.table)

            initEvent()

            viewModel.timeList = viewModel.getTimeList(viewModel.table.timeTable)

            for (i in 1..7) {
                viewModel.getRawCourseByDay(i, viewModel.table.id).observe(this@ScheduleActivity, Observer { list ->
                    if (list == null) return@Observer
                    if (list.isNotEmpty() && list[0].tableId != viewModel.table.id) return@Observer
                    viewModel.allCourseList[i - 1].value = list
                })
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 16) {
            initView()
        }
        if (requestCode == 32 && resultCode == RESULT_OK) {
            drawerLayout.openDrawer(GravityCompat.END)
            AfterImportTipFragment.newInstance().show(supportFragmentManager, "AfterImportTipFragment")
        }
        if (requestCode == 31 && resultCode == RESULT_OK) {
            initView()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        when {
            drawerLayout.isDrawerOpen(GravityCompat.START) -> drawerLayout.closeDrawer(GravityCompat.START)
            drawerLayout.isDrawerOpen(GravityCompat.END) -> drawerLayout.closeDrawer(GravityCompat.END)
            else -> super.onBackPressed()
        }
    }

    override fun onDestroy() {
        AppWidgetUtils.updateWidget(applicationContext)
        super.onDestroy()
    }

}