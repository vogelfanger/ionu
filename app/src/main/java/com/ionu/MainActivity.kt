package com.ionu

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout

import android.support.v4.app.FragmentTransaction
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import io.realm.Realm

import kotlinx.android.synthetic.main.activity_main.*

/**
 * Activity class for the whole application.
 */
class MainActivity : AppCompatActivity(), AlarmsFragment.OnAlarmsFragmentListener,
                        AlarmPeriodFragment.AlarmPeriodFragmentListener,
                        ViewPager.OnPageChangeListener{

    val TAG : String = "MainActivity"
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

    /** Schedules AlarmService based on next available alarm period.
        If there are no enabled alarms, this method does nothing.
        Uses Realm instance, so do not call this inside RealmTransaction.
     */
    private fun scheduleNextAlarm() {
        val nextAlarmMillis = Utils.getNextAlarmMillis(mRealm, System.currentTimeMillis())
        if(nextAlarmMillis == 0L){
            startForegroundService(Intent(applicationContext, AlarmService::class.java))
        }
        else if(nextAlarmMillis > -1){
            Log.d(TAG, "scheduling service to start at " + nextAlarmMillis)
            Utils.scheduleAlarmService(nextAlarmMillis, applicationContext)
        }
    }
}
