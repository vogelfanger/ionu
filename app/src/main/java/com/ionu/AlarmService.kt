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
import io.realm.Sort
import java.util.*

/**
 * Service that monitors device screen during alarm periods.
 * Service remains active as long as there are active alarm periods
 * and shuts itself when no more periods are active.
 * When service shut's itself down, it will reschedule itself to start when the next
 * alarm period becomes active.
 * If user opens screen during an alarm period, a notification is displayed.
 */
class AlarmService : Service() {

    val TAG : String = "AlarmService"
    private var mHandlerThread = HandlerThread("AlarmHandlerThread")
    private lateinit var mRealm : Realm
    private lateinit var mHandler : Handler
    private lateinit var mScreenReceiver : ScreenReceiver
    private lateinit var mPrefs : SharedPreferences
    private var mStartMillis = 0L
    private var mAlarmPeriodViolated = false


    override fun onCreate() {
        Log.d(TAG, "service started")
        startForeground(GlobalVariables.FOREGROUND_NOTIFICATION_ID, getForegroundNotification())
        mStartMillis = System.currentTimeMillis()
        Log.d(TAG, "start time: " + mStartMillis)

        mHandlerThread.start()
        mHandler = Handler(mHandlerThread.looper)
        mPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        mRealm = Realm.getDefaultInstance()

        /* Add change listener so that service will update it's end time etc.
           when AlarmPeriods are edited. */
        mRealm.addChangeListener {
            var currentMillis = System.currentTimeMillis()
            var totalPeriodMillis = getTotalAlarmMillis(mRealm, currentMillis)
            if(totalPeriodMillis == 0L) {
                Log.d(TAG, "no active alarms, stopping service")
                mHandler.removeCallbacks(mStopService)
                stopSelf()
            } else{
                // Schedule service to end when alarm is no longer active
                Log.d(TAG, "scheduling new service end " + totalPeriodMillis/1000L + " seconds from now")
                mHandler.removeCallbacks(mStopService)
                mHandler.postDelayed(mStopService, totalPeriodMillis)
            }
        }

        var currentMillis = System.currentTimeMillis()
        var totalPeriodMillis = getTotalAlarmMillis(mRealm, currentMillis)
        if(totalPeriodMillis == 0L) {
            Log.d(TAG, "no active alarms, stopping service")
            mHandler.removeCallbacks(mStopService)
            stopSelf()
        } else{
            // Schedule service to end when alarm is no longer active
            Log.d(TAG, "scheduling new service end " + totalPeriodMillis/1000L + " seconds from now")
            mHandler.removeCallbacks(mStopService)
            mHandler.postDelayed(mStopService, totalPeriodMillis)
        }

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
                Log.d(TAG, "screen on")
                // if service is still running without enabled alarms, shut it down
                var currentMillis = System.currentTimeMillis()
                if(getTotalAlarmMillis(mRealm, currentMillis) == 0L) {
                    mHandler.removeCallbacks(mStopService)
                    mHandler.post(mStopService)
                }
            }
            GlobalVariables.ACTION_USER_PRESENT -> {
                Log.d(TAG, "user present")
                // make sure there are currently active alarms before adding a violation
                var currentMillis = System.currentTimeMillis()
                if(getTotalAlarmMillis(mRealm, currentMillis) > 0L) {
                    mAlarmPeriodViolated = true
                    showViolationNotification()
                }
                mHandler.removeCallbacks(mStopService)
                mHandler.post(mStopService)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    /**
     * Saves HistoryPeriod to Realm, schedules next service start and stops the service.
     */
    private val mStopService = Runnable {
        Log.d(TAG, "stopping service, no violations")
        val endMillis = System.currentTimeMillis()
        var realm = Realm.getDefaultInstance()
        realm.executeTransaction {
            it.insert(HistoryPeriod(mStartMillis, endMillis, !mAlarmPeriodViolated))
            Log.d(TAG, "History period saved, start time " + mStartMillis
                    + ", end time " + endMillis + ", successful: " + !mAlarmPeriodViolated)
        }
        // Update data stored in shared prefs (for History view)
        updatePrefs(realm)
        // schedule service to start again when next period is active
        val nextStartMillis = Utils.getNextAlarmMillis(realm, endMillis)
        realm.close()
        Utils.scheduleAlarmService(nextStartMillis, applicationContext)
        this.stopSelf()
    }

    //TODO remove if no longer needed
    private fun alarmPeriodViolated() {
        val calendar = Calendar.getInstance()
        val endMillis = calendar.timeInMillis
        mRealm.executeTransaction {
            it.insert(HistoryPeriod(mStartMillis, endMillis, false))
            Log.d("AlarmService", "History period saved, start time " + mStartMillis
                + ", end time " + endMillis + ", successful: " + false)
        }
        // let user know alarm was violated
        showViolationNotification()
    }

    // Update data stored in shared prefs
    private fun updatePrefs(realm : Realm) {
        updateAlltimeData(mPrefs, realm)
        updateWeeklyData(mPrefs, Calendar.getInstance(), realm)
        updateMonthlyData(mPrefs, Calendar.getInstance(), realm)
    }

    /**
     * Updates all-time history data based on alarm periods that were active during
     * service's lifetime.
     * @param prefs preferences where data will be saved
     * @param realm Realm instance that contains alarm periods
     */
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

    /**
     * Updates monthly history data based on alarm periods that were active during
     * service's lifetime.
     * @param prefs preferences where data will be saved
     * @param realm Realm instance that contains alarm periods
     */
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

    /**
     * Updates weekly history data based on alarm periods that were active during
     * service's lifetime.
     * @param prefs preferences where data will be saved
     * @param realm Realm instance that contains alarm periods
     */
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


    /**
     * Returns combined length of currently active alarm periods.
     * Combined period starts from given time and ends when all overlapping,
     * active periods starting after that are over (or there is a break in between alarms).
     * If no alarms are currently active, returns 0.
     * Do not call this if the given realm instance is already in transaction.
     * @param realm Realm instance that contains alarm periods
     * @param currentMillis Start time, from which the combined period is calculated
     * @return combined length of overlapping periods (as millis), starting from currentMillis
     */
    fun getTotalAlarmMillis(realm : Realm, currentMillis : Long) : Long{

        var enabledAlarms = realm.where(AlarmPeriod::class.java).equalTo("enabled", true).findAll()
        if(enabledAlarms.isEmpty()) return 0L

        var totalAlarmMillis = 0L

        // get current time as the same minute representation as used in AlarmPeriod
        var calendar = Calendar.getInstance()
        calendar.timeInMillis = currentMillis
        var currentMinutes = calendar.get(Calendar.HOUR_OF_DAY)*60 + calendar.get(Calendar.MINUTE)
        var leftoverMillis = calendar.get(Calendar.SECOND)*1000L + calendar.get(Calendar.MILLISECOND)

        /* Use the minute format to calculate total length of current alarm session.
         * In case alarms go over midnight, use extended end time so they can be calculated properly
         * (e.g. 22-03 is counted as 22-27) */
        var sortedAlarms = enabledAlarms.sort("startMinutes", Sort.ASCENDING)

        var alarmStart = 0
        var alarmEnd = 0
        var latestEndTime = 0

        // counting total time needs to start from alarms that are currently active, find their index
        var alarmsBeforeCurrentTime = 0
        var alarmsFound = false
        for(alarm : AlarmPeriod in sortedAlarms) {
            if(alarm.startMinutes < alarm.endMinutes) {
                if(alarm.startMinutes <= currentMinutes && currentMinutes < alarm.endMinutes) {
                    alarmsFound = true
                    break
                } else {
                    alarmsBeforeCurrentTime++
                }
            }
            else if(alarm.endMinutes < alarm.startMinutes){
                // period goes over midnight
                if(currentMinutes < alarm.startMinutes && alarm.endMinutes < currentMinutes ) {
                    alarmsBeforeCurrentTime++
                } else {
                    alarmsFound = true
                    break
                }
            }
        }

        if(!alarmsFound){
            // no alarm is currently active
            return 0L
        }

        // add first alarm's length to total time
        val firstAlarm = sortedAlarms[alarmsBeforeCurrentTime]
        if(firstAlarm != null){
            if(firstAlarm.startMinutes > firstAlarm.endMinutes){
                latestEndTime = firstAlarm.endMinutes + 1440
            }else{
                latestEndTime = firstAlarm.endMinutes
            }
            totalAlarmMillis += (latestEndTime - currentMinutes)*60*1000L
        }

        // go through the rest of the alarms and keep adding to end time until there is a break in between alarms
        alarmsBeforeCurrentTime++
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
                    totalAlarmMillis += (alarmEnd - latestEndTime)*60*1000L
                    latestEndTime = alarmEnd
                }
            }else{
                // there is a break in between alarms, total alarm time ends here
                return totalAlarmMillis - leftoverMillis
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
                        totalAlarmMillis += (alarmEnd - latestEndTime)*1000L*60
                        latestEndTime = alarmEnd
                    }
                }else{
                    // there is a break in between alarms, total alarm time ends here
                    return totalAlarmMillis - leftoverMillis
                }
            }
        }
        return totalAlarmMillis - leftoverMillis
    }

    private fun getRemainingMinutes() : Int {
        var minutesRemaining = 0

        return minutesRemaining
    }

    private fun getForegroundNotification() : Notification {
        val builder = NotificationCompat.Builder(this, GlobalVariables.MAIN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(resources.getString(R.string.notification_title_alarms_active))
            .setContentText(resources.getString(R.string.notification_content_alarms_active))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        return builder.build()
    }

    private fun showViolationNotification() {
        val builder = NotificationCompat.Builder(this, GlobalVariables.MAIN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(resources.getString(R.string.notification_title_period_violated))
            .setContentText(resources.getString(R.string.notification_content_period_violated))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(GlobalVariables.ALARM_PERIOD_VIOLATED_NOTIFICATION_ID, builder.build())
    }

    //TODO remove if no longer needed
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
