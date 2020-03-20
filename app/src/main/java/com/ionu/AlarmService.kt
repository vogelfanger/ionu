package com.ionu

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.util.Log
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import java.util.*

class AlarmService : Service() {

    private var mHandlerThread = HandlerThread("AlarmHandlerThread")
    private lateinit var mRealm : Realm
    private lateinit var mHandler : Handler
    private lateinit var mScreenReceiver : ScreenReceiver
    private lateinit var mAlarms : RealmResults<AlarmPeriod>
    private lateinit var mPrefs : SharedPreferences
    private var mStartMillis = 0L


    override fun onCreate() {
        Log.d("AlarmService", "onCreate()")
        startForeground(GlobalVariables.FOREGROUND_NOTIFICATION_ID, getForegroundNotification())
        mStartMillis = Calendar.getInstance().timeInMillis
        Log.d("AlarmService", "start time: " + mStartMillis)

        mHandlerThread.start()
        mHandler = Handler(mHandlerThread.looper)
        mPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        mRealm = Realm.getDefaultInstance()

        var totalAlarmTimeMillis : Long = 0

        mRealm.executeTransaction {
            // find enabled alarms
            mAlarms = it.where(AlarmPeriod::class.java).equalTo("enabled", true).findAll()
        }

        if(mAlarms.isNotEmpty()){
            totalAlarmTimeMillis = 1000L * 60 * findTotalAlarmMinutes(mAlarms)

            mAlarms.addChangeListener { _ ->
                if(mAlarms.isEmpty()){
                    Log.d("AlarmService", "no more active alarms, stopping service")
                    mHandler.removeCallbacks(mStopService)
                    stopSelf()
                }else{
                    // reschedule service end time
                    var newAlarmTimeMillis = 1000L * 60 * findTotalAlarmMinutes(mAlarms)
                    Log.d("AlarmService", "scheduling new service end " + newAlarmTimeMillis/1000 + " seconds from now")
                    mHandler.removeCallbacks(mStopService)
                    mHandler.postDelayed(mStopService, newAlarmTimeMillis)
                }
            }
        }

        // Schedule service to end when alarm is no longer active
        Log.d("AlarmService", "scheduling service end " + totalAlarmTimeMillis/1000 + " seconds from now")
        mHandler.postDelayed(mStopService, totalAlarmTimeMillis)

        mScreenReceiver = ScreenReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(mScreenReceiver, filter)

    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacks(mStopService)
        unregisterReceiver(mScreenReceiver)
        mRealm.close()
        mHandlerThread.quitSafely()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        var action = ""
        intent?.let {
            if(it.action != null){
                action = it.action
            }
        }
        when(action){

            GlobalVariables.ACTION_SCREEN_ON -> {
                Log.d("AlarmService", "onStartCommand() screen on")
                // if service is still running without enabled alarms, shut it down
                if(findTotalAlarmMinutes(mAlarms) == 0) {
                    mHandler.post(mStopService)
                }
            }
            GlobalVariables.ACTION_SCREEN_OFF -> {
                Log.d("AlarmService", "onStartCommand() screen off")
            }
            GlobalVariables.ACTION_USER_PRESENT -> {
                Log.d("AlarmService", "onStartCommand() user present")
                // make sure there are currently active alarms before adding a violation
                if(findTotalAlarmMinutes(mAlarms) == 0) {
                    mHandler.post(mStopService)
                } else {
                    alarmPeriodViolated()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    private val mStopService = Runnable {
        Log.d("AlarmService", "stopping service, no more alarms active")
        val endMillis = Calendar.getInstance().timeInMillis
        mRealm.executeTransaction {
            it.insert(HistoryPeriod(mStartMillis, endMillis, true))
            Log.d("AlarmService", "History period saved, start time " + mStartMillis
                    + ", end time " + endMillis + ", successful: " + true)
        }
        // TODO schedule next alarm
        this.stopSelf()
    }

    private fun alarmPeriodViolated() {
        val calendar = Calendar.getInstance()
        val endMillis = calendar.timeInMillis
        mRealm.executeTransaction {
            it.insert(HistoryPeriod(mStartMillis, endMillis, false))
            Log.d("AlarmService", "History period saved, start time " + mStartMillis
                + ", end time " + endMillis + ", successful: " + false)
        }

        // let user know alarm was violated
        val builder = NotificationCompat.Builder(this, GlobalVariables.MAIN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(resources.getString(R.string.app_name))
            .setContentText(resources.getString(R.string.notification_alarm_period_violated))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(GlobalVariables.ALARM_PERIOD_VIOLATED_NOTIFICATION_ID, builder.build())

        // Update data stored in shared prefs
        updatePrefs()
        stopSelf()
    }

    // Update data stored in shared prefs
    private fun updatePrefs() {
        updateAlltimeData(mPrefs, mRealm)
        updateWeeklyData(mPrefs, Calendar.getInstance(), mRealm)
        updateMonthlyData(mPrefs, Calendar.getInstance(), mRealm)
    }

    fun updateAlltimeData(prefs: SharedPreferences, realm: Realm) {

        var totalTime = 0L
        realm.executeTransaction {
            var pastPeriods = it.where(HistoryPeriod::class.java).findAll()

            // update alltime total
            for (pastPeriod in pastPeriods) {
                totalTime += pastPeriod.getLenght()
            }
            prefs.edit().putLong(GlobalVariables.PREF_KEY_ALLTIME_TOTAL_TIME, totalTime).apply()

            var sortedPeriods = pastPeriods.sort("endMillis", Sort.DESCENDING)
            var currentStreak = 0L
            for (period in sortedPeriods){
                if(period.successful) {
                    currentStreak += period.getLenght()
                } else break
            }
            prefs.edit().putLong(GlobalVariables.PREF_KEY_CURRENT_STREAK, currentStreak).apply()
            if(currentStreak > prefs.getLong(GlobalVariables.PREF_KEY_BEST_STREAK, 0L)) {
                prefs.edit().putLong(GlobalVariables.PREF_KEY_BEST_STREAK, currentStreak).apply()
            }
        }
    }

    fun updateMonthlyData(prefs: SharedPreferences, calendar: Calendar, realm: Realm) {

        // get start of this month in milliseconds
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.clear(Calendar.MINUTE)
        calendar.clear(Calendar.SECOND)
        calendar.clear(Calendar.MILLISECOND)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        var startOfMonth = calendar.timeInMillis

        realm.executeTransaction {
            // find all alarms that ended this month
            var periodsThisMonth = it.where(HistoryPeriod::class.java)
                .greaterThanOrEqualTo("endMillis", startOfMonth).findAll()

            // in case of first entry, check if it started last month
            if(periodsThisMonth.size == 1) {
                var lastPeriod = periodsThisMonth.first()
                if(lastPeriod != null && lastPeriod.startMillis < startOfMonth) {

                    // add first part to previous month and see if it was a new best
                    var lastMonthTotal = prefs.getLong(GlobalVariables.PREF_KEY_CURRENT_MONTH, 0L)
                    lastMonthTotal += startOfMonth - lastPeriod.startMillis
                    if(lastMonthTotal > prefs.getLong(GlobalVariables.PREF_KEY_BEST_MONTH, 0L)) {
                        prefs.edit().putLong(GlobalVariables.PREF_KEY_BEST_MONTH, lastMonthTotal).apply()
                    }

                    // reset this month's time to the remaining part of the alarm
                    var thisMonthTime = lastPeriod.endMillis - startOfMonth
                    prefs.edit().putLong(GlobalVariables.PREF_KEY_CURRENT_MONTH, thisMonthTime).apply()
                    if(thisMonthTime > prefs.getLong(GlobalVariables.PREF_KEY_BEST_MONTH, 0L)) {
                        prefs.edit().putLong(GlobalVariables.PREF_KEY_BEST_MONTH, thisMonthTime).apply()
                    }
                }
                else if(lastPeriod != null) {
                    // alarm started and ended this month, add to this month's data and best if needed
                    prefs.edit().putLong(GlobalVariables.PREF_KEY_CURRENT_MONTH, lastPeriod.getLenght()).apply()
                    if(lastPeriod.getLenght() > prefs.getLong(GlobalVariables.PREF_KEY_BEST_MONTH, 0L)) {
                        prefs.edit().putLong(GlobalVariables.PREF_KEY_BEST_MONTH, lastPeriod.getLenght()).apply()
                    }
                }
            }

            // add alarms to current month's time and update monthly best if needed
            else {
                var thisMonthTotal = 0L
                for(period in periodsThisMonth) {
                    if(period != null) {
                        if(period.startMillis < startOfMonth) {
                            thisMonthTotal += (period.endMillis - startOfMonth)
                        }else {
                            thisMonthTotal += period.getLenght()
                        }
                    }
                }
                // update weekly total and best week
                if(thisMonthTotal > prefs.getLong(GlobalVariables.PREF_KEY_BEST_MONTH, 0L)) {
                    prefs.edit().putLong(GlobalVariables.PREF_KEY_BEST_MONTH, thisMonthTotal).apply()
                }
                prefs.edit().putLong(GlobalVariables.PREF_KEY_CURRENT_MONTH, thisMonthTotal).apply()
            }

            // update failure count
            var failedPeriods = periodsThisMonth.where().equalTo("successful", false)
            prefs.edit().putLong(GlobalVariables.PREF_KEY_MONTHLY_FAILURES, failedPeriods.count()).apply()
        }
    }

    fun updateWeeklyData(prefs: SharedPreferences, calendar: Calendar, realm: Realm) {

        // get start of this week in milliseconds
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.clear(Calendar.MINUTE)
        calendar.clear(Calendar.SECOND)
        calendar.clear(Calendar.MILLISECOND)
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        var startOfWeek = calendar.timeInMillis

        realm.executeTransaction {
            // find all alarms that ended this week
            var periodsThisWeek = it.where(HistoryPeriod::class.java)
                .greaterThanOrEqualTo("endMillis", startOfWeek).findAll()

            // in case of first entry, check if it started last week
            if(periodsThisWeek.size == 1) {
                var lastPeriod = periodsThisWeek.first()
                if(lastPeriod != null && lastPeriod.startMillis < startOfWeek) {

                    // add first part to previous week and see if it was a new best
                    var lastWeekTotal = prefs.getLong(GlobalVariables.PREF_KEY_CURRENT_WEEK, 0L)
                    lastWeekTotal += startOfWeek - lastPeriod.startMillis
                    if(lastWeekTotal > prefs.getLong(GlobalVariables.PREF_KEY_BEST_WEEK, 0L)) {
                        prefs.edit().putLong(GlobalVariables.PREF_KEY_BEST_WEEK, lastWeekTotal).apply()
                    }

                    // reset this week's time to the remaining part of the alarm
                    var thisWeekTime = lastPeriod.endMillis - startOfWeek
                    prefs.edit().putLong(GlobalVariables.PREF_KEY_CURRENT_WEEK, thisWeekTime).apply()
                    if(thisWeekTime > prefs.getLong(GlobalVariables.PREF_KEY_BEST_WEEK, 0L)) {
                        prefs.edit().putLong(GlobalVariables.PREF_KEY_BEST_WEEK, thisWeekTime).apply()
                    }
                }
                else if(lastPeriod != null) {
                    // alarm started and ended this week, add to this week's data and best if needed
                    prefs.edit().putLong(GlobalVariables.PREF_KEY_CURRENT_WEEK, lastPeriod.getLenght()).apply()
                    if(lastPeriod.getLenght() > prefs.getLong(GlobalVariables.PREF_KEY_BEST_WEEK, 0L)) {
                        prefs.edit().putLong(GlobalVariables.PREF_KEY_BEST_WEEK, lastPeriod.getLenght()).apply()
                    }
                }
            }

            // add alarms to current week's time and update weekly best if needed
            else {
                var thisWeekTotal = 0L
                for(period in periodsThisWeek) {
                    if(period != null) {
                        if(period.startMillis < startOfWeek) {
                            thisWeekTotal += (period.endMillis - startOfWeek)
                        }else {
                            thisWeekTotal += period.getLenght()
                        }
                    }
                }
                // update weekly total and best week
                if(thisWeekTotal > prefs.getLong(GlobalVariables.PREF_KEY_BEST_WEEK, 0L)) {
                    prefs.edit().putLong(GlobalVariables.PREF_KEY_BEST_WEEK, thisWeekTotal).apply()
                }
                prefs.edit().putLong(GlobalVariables.PREF_KEY_CURRENT_WEEK, thisWeekTotal).apply()
            }

            // update failure count
            var failedPeriods = periodsThisWeek.where().equalTo("successful", false)
            prefs.edit().putLong(GlobalVariables.PREF_KEY_WEEKLY_FAILURES, failedPeriods.count()).apply()
        }
    }


    /* Returns total alarm length using given RealmResults.
       Length is given as minutes the combined alarm should last from current time.
       If no alarms are currently active, returns 0
     */
    fun findTotalAlarmMinutes(alarms: RealmResults<AlarmPeriod>) : Int{
        if(alarms.isEmpty()) return 0

        var totalAlarmMinutes = 0

        // get current time as the same minute representation as used in AlarmPeriod
        var calendar = Calendar.getInstance()
        var currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        /**
         * Use the minute format to calculate total length of current alarm session.
         * In case alarms go over midnight, use extended end time so they can be calculated properly
         * (e.g. 22-03 is counted as 22-27).
         */
        var sortedAlarms = alarms.sort("startMinutes", Sort.ASCENDING)

        var alarmStart = 0
        var alarmEnd = 0
        var latestEndTime = 0

        // counting total time needs to start from alarms that are currently active, find their index
        var alarmsBeforeCurrentTime = 0
        var alarmsFound = false
        for(alarm : AlarmPeriod in sortedAlarms) {
            if(alarm.startMinutes <= currentMinutes && alarm.endMinutes >= currentMinutes){
                // index found, break
                alarmsFound = true
                break
            }else{
                alarmsBeforeCurrentTime++
            }
        }
        if(!alarmsFound){
            // no alarm is currently active
            return 0
        }

        // add first alarm's length to total time
        val firstAlarm = sortedAlarms[alarmsBeforeCurrentTime]
        if(firstAlarm != null){
            if(firstAlarm.startMinutes > firstAlarm.endMinutes){
                latestEndTime = firstAlarm.startMinutes + 1440
            }else{
                latestEndTime = firstAlarm.endMinutes
            }
            totalAlarmMinutes += latestEndTime - currentMinutes
        }

        // go through the rest of the alarms and keep adding to end time until there is a break in between alarms
        for(i in alarmsBeforeCurrentTime until sortedAlarms.size){
            // check if alarm goes over midnight and use extended end time if necessary
            var alarm = sortedAlarms[i]
            if(alarm != null){
                alarmStart = alarm.startMinutes
                if(alarm.startMinutes > alarm.endMinutes){
                    alarmEnd = alarm.endMinutes + 1440
                }else {
                    alarmEnd = alarm.endMinutes
                }
            }

            // see if alarm overlaps with any previous alarms
            if(latestEndTime >= alarmStart) {
                // only add to total time if no previous alarms had later end time
                if(alarmEnd > latestEndTime){
                    totalAlarmMinutes += alarmEnd - latestEndTime
                    latestEndTime = alarmEnd
                }
            }else{
                // there is a break in between alarms, total alarm time ends here
                return totalAlarmMinutes
            }

        } //for

        // There are no breaks in between alarms yet, process alarms that were left at the start of list
        for (i in 0 until alarmsBeforeCurrentTime){
            var alarm = sortedAlarms[i]
            if(alarm != null){
                // alarms are already extending over midnight, use extended start and end time
                alarmStart = alarm.startMinutes + 1440
                alarmEnd = alarm.endMinutes + 1440

                // see if alarm overlaps with any previous alarms
                if(latestEndTime >= alarmStart) {
                    // only add to total time if no previous alarms had later end time
                    if(alarmEnd > latestEndTime){
                        totalAlarmMinutes += alarmEnd - latestEndTime
                        latestEndTime = alarmEnd
                    }
                }else{
                    // there is a break in between alarms, total alarm time ends here
                    return totalAlarmMinutes
                }
            }
        }
        return totalAlarmMinutes
    }

    private fun getRemainingMinutes() : Int {
        var minutesRemaining = 0

        return minutesRemaining
    }

    private fun getForegroundNotification() : Notification {
        val builder = NotificationCompat.Builder(this, GlobalVariables.MAIN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(resources.getString(R.string.app_name))
            .setContentText(resources.getString(R.string.notification_alarms_active))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        return builder.build()
    }

    // Updates the foreground notification with remaining alarm time.
    private fun updateNotificationTime() {

        var remainingTime = getRemainingMinutes()
        var remainingHours : Int = remainingTime/60
        var remainingMinutes : Int = remainingTime - (remainingHours*60)
        var time = "" + remainingHours + "h " + remainingMinutes + "min"

        val builder = NotificationCompat.Builder(this, GlobalVariables.MAIN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(resources.getString(R.string.app_name))
            .setContentText(resources.getString(R.string.notification_time_remaining) + time)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        var nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(GlobalVariables.FOREGROUND_NOTIFICATION_ID, builder.build())
    }
}
