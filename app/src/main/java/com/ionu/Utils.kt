package com.ionu

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import java.util.*

class Utils{

    companion object {

        /**
         * Finds the time in millis when next AlarmPeriod should start. Do not call inside RealmTransaction.
         * @param realm Realm where AlarmPeriods are searched from
         * @param currentMillis Current time, next AlarmPeriod will be the one starting after this time.
         * @return -1 if no enabled alarms were found,
         *          0 if alarm should start right away,
         *          or time in millis when next alarm starts.
         */
        fun getNextAlarmMillis(realm: Realm, currentMillis: Long) : Long{
            var ret = -1L
            var calendar = Calendar.getInstance()
            calendar.timeInMillis = currentMillis
            var currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            var millisSinceMidnight = currentMillis - calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            var millisToMidnight = calendar.timeInMillis - currentMillis

            realm.executeTransaction {
                var activeAlarms = it.where(AlarmPeriod::class.java).equalTo("enabled", true).findAll()

                if(activeAlarms.isEmpty()){
                    ret = -1L
                } else{
                    // check if there are currently active alarms
                    var currentAlarms = activeAlarms.where()
                        .lessThanOrEqualTo("startMinutes", currentMinutes)
                        .and().greaterThanOrEqualTo("endMinutes", currentMinutes).findAll()
                    if(currentAlarms.isNotEmpty()){
                        // alarm should be activated right away
                        ret = 0L
                    }else{
                        // check if there are any alarms starting later this day
                        var nextAlarmsToday = activeAlarms.where().
                            greaterThanOrEqualTo("startMinutes", currentMinutes).findAll()

                        if(nextAlarmsToday.isNotEmpty()){
                            var nextAlarmsTodaySorted = nextAlarmsToday.sort("startMinutes", Sort.ASCENDING)
                            var nextAlarm = nextAlarmsTodaySorted.first()
                            if(nextAlarm != null){
                                ret = currentMillis - millisSinceMidnight + 1000L * 60 * nextAlarm.startMinutes
                            }
                        }else{
                            // no alarms today, get the first one starting tomorrow
                            var activeAlarmsSorted = activeAlarms.sort("startMinutes", Sort.ASCENDING)
                            var nextAlarm = activeAlarmsSorted.first()
                            if(nextAlarm != null){
                                ret = currentMillis + millisToMidnight + 1000L * 60 * nextAlarm.startMinutes
                            }
                        }
                    }
                }
            }
            return ret
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

        /**
         * Schedules AlarmService to start at a specified time.
         * If service was already scheduled to start at a different time,
         * that previous scheduling will cancelled and new trigger time will be used instead.
         * @param triggerAtMillis Time in millis when the service should start
         * @param context Context used when creating intent to AlarmService
         */
        fun scheduleAlarmService(triggerAtMillis: Long, context : Context){

            // keep only one alarm at a time, cancel previous one if it exists
            var serviceIntent = Intent(context, AlarmService::class.java)
            var pendingIntent = PendingIntent.getForegroundService(context,
                GlobalVariables.ALARM_SERVICE_REQUEST_CODE, serviceIntent, PendingIntent.FLAG_CANCEL_CURRENT)

            var alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }

    } // companion object
}

