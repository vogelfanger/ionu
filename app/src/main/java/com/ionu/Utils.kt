package com.ionu

import android.app.AlarmManager
import android.app.PendingIntent
import android.util.Log
import io.realm.RealmResults
import io.realm.Sort
import java.util.*

class Utils{

    companion object {

        /*
           Schedules a PendingÍntent to happen at given time,
           if no other intents are already scheduled before it.
           If other intent is already scheduled for earlier time, this method does nothing.
        */
        fun ScheduleRCTAlarm(pendingIntent: PendingIntent,
                             triggerAtMinutes: Int, alarmManager: AlarmManager){

            //TODO check previously scheduled alarm and replace it only if it has later start time
            val triggerAtMillis = 1000L * 60 * triggerAtMinutes

            Log.d("IONU", "Scheduling new alarm")
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }

        /**
         * Iterates through given collection of alarms
         * and checks if adding new alarm there would create an endless 24h cycle.
         * @param newAlarm Alarm that is checked against the collection
         * @param enabledAlarms collection against which comparison is made, do Realm query to get this.
         * @return false, if adding new alarm would create endless 24h cycle, otherwise true
         */
        fun isNewAlarmValid(newAlarm : AlarmPeriod, enabledAlarms: RealmResults<AlarmPeriod>) : Boolean {

            // no enabled alarms, only check that the new alarm doesn't exceed limit
            if(enabledAlarms.isEmpty()){
                return newAlarm.lengthInMinutes() < 1440
            }

            var alarmValid = true
            var totalAlarmMinutes = 0

            var sortedAlarms = enabledAlarms.sort("startMinutes", Sort.ASCENDING)
            var counter = sortedAlarms.size
            var latestEndTime = 0

            var alarmStart = 0
            var alarmEnd = 0
            var newAlarmStart = 0
            var newAlarmEnd = 0

            // if alarm goes over midnight, use extended end time to calculate the span
            newAlarmStart = newAlarm.startMinutes
            if(newAlarm.startMinutes > newAlarm.endMinutes){
                newAlarmEnd = newAlarm.endMinutes + 1440
            }else{
                newAlarmEnd = newAlarm.endMinutes
            }

            // add first alarm's length to total time and set it initially as the one that ends last
            val firstAlarm = sortedAlarms[0]
            if(firstAlarm != null){
                totalAlarmMinutes += firstAlarm.lengthInMinutes()
                if(firstAlarm.startMinutes > firstAlarm.endMinutes){
                    latestEndTime = firstAlarm.endMinutes + 1440
                }else{
                    latestEndTime = firstAlarm.endMinutes
                }
            }

            var newAlarmUsed = false
            for (alarm: AlarmPeriod in sortedAlarms){

                // check if alarm goes over midnight and extend end time if necessary
                alarmStart = alarm.startMinutes
                if(alarm.startMinutes > alarm.endMinutes){
                    alarmEnd = alarm.endMinutes + 1440
                }else {
                    alarmEnd = alarm.endMinutes
                }

                // see if alarm overlaps with any previous ones
                if(latestEndTime >= alarmStart) {
                    // only add to total time if no previous alarms had later end time
                    if(alarmEnd > latestEndTime){
                        totalAlarmMinutes += alarmEnd - latestEndTime
                        latestEndTime = alarmEnd
                    }
                }
                // the two alarms were not overlapping, see if the new alarm would fill the hole
                else if(latestEndTime >= newAlarmStart){
                    // new alarm fills the hole, add to total time using the alarm that ends last
                    if(newAlarmEnd > alarmEnd){
                        totalAlarmMinutes += newAlarmEnd - latestEndTime
                        latestEndTime = newAlarmEnd
                    }else{
                        totalAlarmMinutes += alarmEnd - latestEndTime
                        latestEndTime = alarmEnd
                    }
                    newAlarmUsed = true
                }
                else{
                    // There is a break in between alarms, return
                    alarmValid = true
                    break
                }

                // special case: all enabled alarms are now processed
                if(counter == 1){
                    if(newAlarmUsed){
                        // make sure total time is counted using alarm that ends last
                        if(newAlarmEnd > alarmEnd){
                            // iterated alarm was already set as latest alarm, only add the difference of new alarm
                            totalAlarmMinutes += newAlarmEnd - latestEndTime
                        }
                    }else{
                        // new alarm could still extend the total alarm time, check if it stacks with other alarms
                        if(newAlarmStart <= latestEndTime && newAlarmEnd > latestEndTime){
                            totalAlarmMinutes += newAlarmEnd - latestEndTime
                        }else if(newAlarmStart+1440 <= latestEndTime && newAlarmEnd+1440 > latestEndTime){
                            totalAlarmMinutes += newAlarmEnd+1440 - latestEndTime
                        }
                    }
                }

                // make sure total alarm time doesn't exceed 24h limit
                if(totalAlarmMinutes >= 1440){
                    alarmValid = false
                    break
                }
                counter--

            } //for

            return alarmValid
        }
    }
}

