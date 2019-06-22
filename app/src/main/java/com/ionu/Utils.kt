package com.ionu

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import java.util.*

public fun ScheduleRCTAlarm(pendingIntent: PendingIntent,
                         triggerAtMillis: Long, alarmManager: AlarmManager){

    Log.d("IONU", "Scheduling new alarm ")
    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, triggerAtMillis, pendingIntent)
}