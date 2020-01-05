package com.ionu

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
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

    private lateinit var mAlarms : RealmResults<AlarmPeriod>

    override fun onCreate() {
        Log.d("AlarmService", "onCreate()")
        startForeground(GlobalVariables.FOREGROUND_NOTIFICATION_ID, getForegroundNotification())

        mHandlerThread.start()
        mHandler = Handler(mHandlerThread.looper)
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
    }

    override fun onDestroy() {
        super.onDestroy()
        mRealm.close()
        mHandler.removeCallbacks(mStopService)
        mHandlerThread.quitSafely()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO command for starting service when alarms are edited/created/deleted
        // TODO find new total alarm minutes and reschedule mStopService
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    private val mStopService = Runnable {
        Log.d("AlarmService", "stopping service, no more alarms active")
        this.stopSelf()
    }


    /* Returns total alarm length using given RealmResults.
       Length is given as minutes the combined alarm should last from current time.
       If no alarms are currently active, returns 0
     */
    private fun findTotalAlarmMinutes(alarms: RealmResults<AlarmPeriod>) : Int{
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

    private fun getForegroundNotification() : Notification {
        val builder = NotificationCompat.Builder(this, GlobalVariables.MAIN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(resources.getString(R.string.app_name))
            .setContentText(resources.getString(R.string.notification_alarms_active))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        return builder.build()
    }
}
