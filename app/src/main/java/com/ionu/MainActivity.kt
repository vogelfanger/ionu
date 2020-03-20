package com.ionu

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout

import android.support.v4.app.FragmentTransaction
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity;
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.internal.Util

import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), AlarmsFragment.OnAlarmsFragmentListener,
                        AlarmPeriodFragment.AlarmPeriodFragmentListener,
                        ViewPager.OnPageChangeListener{

    private lateinit var mViewPager: ViewPager
    private lateinit var mPagerAdapter: MainPagerAdapter
    private lateinit var mCoordinatorLayout: CoordinatorLayout
    private lateinit var mRealm: Realm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        mRealm = Realm.getDefaultInstance()
        scheduleNextAlarm()

        mCoordinatorLayout = findViewById(R.id.main_activity_coordinator_layout)
        mViewPager = main_viewpager
        mPagerAdapter = MainPagerAdapter(supportFragmentManager, this)
        mViewPager.adapter = mPagerAdapter
        mViewPager.addOnPageChangeListener(this)

        val tabLayout: TabLayout = main_tablayout
        tabLayout.setupWithViewPager(mViewPager)

        fab.setOnClickListener { view ->
            // TODO use current clock time as AlarmPeriod parameter
            // Insert new alarm to Realm, fragments will update the change on their own
            val alarm = AlarmPeriod()
            Realm.getDefaultInstance().use{
                it.executeTransaction(Realm.Transaction {it.insert(alarm)})
            }
            Snackbar.make(view, "New alarm saved", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mRealm.close()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPageSelected(position: Int) {
        // update history view every time it's selected
        var page = mPagerAdapter.getItem(position)
        if(page is HistoryFragment) {
            page.updateView(applicationContext)
            fab.hide()
        }else if(page is PageRootFragment) {
            fab.show()
        }
    }

    override fun onPageScrollStateChanged(state: Int) {

    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onAlarmInListEnabled(alarmID: String, isEnabled: Boolean) {

        // use this switch so that realm transaction gets executed before scheduling service
        var scheduleService = false
        mRealm.executeTransaction {
            var newAlarm = mRealm.where(AlarmPeriod::class.java).equalTo("id", alarmID).findFirst()
            var enabledAlarms = it.where(AlarmPeriod::class.java).equalTo("enabled", true).findAll()

            newAlarm?.let {
                if(isEnabled) {
                    // alarm is being enabled, make sure it doesn't form endless 24h loop with other alarms
                    if(Utils.isNewAlarmValid(it, enabledAlarms)){
                        it.enabled = true
                        scheduleService = true
                    }else{
                        it.enabled = false
                        Snackbar.make(mCoordinatorLayout, "Cannot enable alarms that exceed 24h", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show()
                    }
                }else{
                    // alarm is being disabled, no need to verify anything
                    it.enabled = false
                }
            }
        }
        if(scheduleService){
            // schedule alarm service to start if alarm was enabled
            scheduleNextAlarm()
        }
    }

    override fun onAlarmEnabled(alarmID: String, isEnabled: Boolean) {
        // use this switch so that realm transaction gets executed before scheduling service
        var scheduleService = false
        mRealm.executeTransaction {
            var newAlarm = mRealm.where(AlarmPeriod::class.java).equalTo("id", alarmID).findFirst()
            var enabledAlarms = it.where(AlarmPeriod::class.java).equalTo("enabled", true).findAll()

            newAlarm?.let {
                if(isEnabled) {
                    // alarm is being enabled, make sure it doesn't form endless 24h loop with other alarms
                    if(Utils.isNewAlarmValid(it, enabledAlarms)){
                        it.enabled = true
                        scheduleService = true
                    }else{
                        it.enabled = false
                        Snackbar.make(mCoordinatorLayout, "Cannot enable alarms that exceed 24h", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show()
                    }
                }else{
                    // alarm is being disabled, no need to verify anything
                    it.enabled = false
                }
            }
        }
        if(scheduleService){
            // schedule alarm service to start if alarm was enabled
            scheduleNextAlarm()
        }
    }

    override fun onAlarmSelected(alarmPeriod : AlarmPeriod) {
        Log.d("MainActivity", "onAlarmSelected(), alarmID: " + alarmPeriod.id)
        // Show alarm details in another fragment
        val bundle = Bundle()
        bundle.putString(GlobalVariables.ALARM_PERIOD_ID_BUNDLE, alarmPeriod.id)
        val alarmFragment = AlarmPeriodFragment()
        alarmFragment.arguments = bundle

        val transaction : FragmentTransaction? = supportFragmentManager.beginTransaction()
        transaction?.replace(R.id.page_root_frame, alarmFragment)
        transaction?.addToBackStack(null)
        transaction?.commit()
    }

    override fun onAlarmDeleted() {
        // re-schedule service start if needed
        scheduleNextAlarm()

        val transaction : FragmentTransaction? = supportFragmentManager.beginTransaction()
        transaction?.replace(R.id.page_root_frame, AlarmsFragment())
        transaction?.addToBackStack(null)
        transaction?.commit()
        //TODO implement Undo action??? could give the alarm period as parameter and save it back to Realm on Undo
        Snackbar.make(mCoordinatorLayout, R.string.alarm_deleted_message, Snackbar.LENGTH_LONG).show()
    }

    override fun onAlarmTimeChanged(alarmID: String) {
        // re-schedule service start if needed
        scheduleNextAlarm()
    }

    /**
     * Finds the time when next AlarmPeriod should start. Do not call inside RealmTransaction.
     * @return -1 if no enabled alarms were found,
     *          0 if alarm should start right away,
     *          or time in millis when next alarm starts.
     */
    private fun getNextAlarmMillis() : Long{
        var ret = -1L
        var calendar = Calendar.getInstance()
        var currentMillis = calendar.timeInMillis
        var currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        var millisSinceMidnight = currentMinutes * 60 * 1000L + calendar.get(Calendar.SECOND) * 1000L
            + calendar.get(Calendar.MILLISECOND)

        mRealm.executeTransaction {
            var activeAlarms = it.where(AlarmPeriod::class.java).equalTo("enabled", true).findAll()

            if(activeAlarms.isEmpty()){
                ret = -1L
            } else{
                // check if there are currently active alarms
                var currentAlarms = activeAlarms.where()
                    .lessThanOrEqualTo("startMinutes", currentMinutes)
                    .and().greaterThanOrEqualTo("endMinutes", currentMinutes+2).findAll()
                if(currentAlarms.isNotEmpty()){
                    // alarm should be activated right away
                    ret = 0L
                }else{
                    // check if there are any alarms starting later this day
                    var nextAlarmsToday = activeAlarms.where().
                        greaterThanOrEqualTo("startMinutes", currentMinutes).findAll()

                    if(nextAlarmsToday.isNotEmpty()){
                        nextAlarmsToday.sort("startMinutes", Sort.ASCENDING)
                        var nextAlarm = nextAlarmsToday.first()
                        if(nextAlarm != null){
                            ret = currentMillis - millisSinceMidnight + 1000L * 60 * nextAlarm.startMinutes
                        }
                    }else{
                        // no alarms today, get the first one starting tomorrow
                        var nextAlarm = activeAlarms.sort("startMinutes", Sort.ASCENDING).first()
                        if(nextAlarm != null){
                            var timeLeftToday = (1440 - currentMinutes) * 1000L * 60
                            ret = 1000L * 60 * nextAlarm.startMinutes + timeLeftToday + currentMillis
                        }
                    }
                }
            }
        }
        Log.d("MainActivity", "getNextAlarmMillis() ret: " + ret + ", currentMillis: " + currentMillis)
        return ret
    }

    /** Schedules AlarmService based on next available alarm period.
        If there are no enabled alarms, this method does nothing.
        Uses Realm instance, so do not call inside RealmTransaction.
     */
    private fun scheduleNextAlarm() {
        val nextAlarmMillis = getNextAlarmMillis()
        if(nextAlarmMillis == 0L){
            startForegroundService(Intent(applicationContext, AlarmService::class.java))
        }
        else if(nextAlarmMillis > -1){
            scheduleAlarmService(nextAlarmMillis)
        }
    }

    /** Schedules AlarmService to start at specified time.
        Uses Realm instance, so do not call inside RealmTransaction.
    */
    private fun scheduleAlarmService(triggerAtMillis: Long){

        Log.d("MainActivity", "scheduleAlarmService() at " + triggerAtMillis)

        // keep only one alarm at a time, cancel previous one if it exists
        var serviceIntent = Intent(applicationContext, AlarmService::class.java)
        var pendingIntent = PendingIntent.getForegroundService(applicationContext,
            GlobalVariables.ALARM_SERVICE_REQUEST_CODE, serviceIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        var alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    // Starts AlarmService immediately
    private fun startAlarmService(){
        startForegroundService(Intent(applicationContext, AlarmService::class.java))
    }

}
