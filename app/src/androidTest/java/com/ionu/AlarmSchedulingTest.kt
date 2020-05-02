package com.ionu

import android.content.Context
import android.content.SharedPreferences
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import io.realm.Realm
import io.realm.RealmConfiguration

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import java.util.*

/**
 * Tests that AlarmService is scheduled to start and end at correct time.
 * Instrumented test, which will execute on an Android device.
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class AlarmSchedulingTest {

    lateinit var mTestRealm: Realm
    lateinit var mPrefs: SharedPreferences
    lateinit var mContext: Context
    val PREF_TAG = "AlarmSchedulingTest"

    @Before
    fun setup() {
        val testConfig = RealmConfiguration.Builder().inMemory().name("test-realm").build()
        mTestRealm = Realm.getInstance(testConfig)
        mTestRealm.executeTransaction {
            it.deleteAll()
        }
        mContext = InstrumentationRegistry.getTargetContext()
        mPrefs = mContext.getSharedPreferences(PREF_TAG, Context.MODE_PRIVATE)
        mPrefs.edit().clear().apply()
    }


    /**
     * Tests that AlarmService is scheduled to start at correct time from activity.
     */
    @Test
    fun scheduleAlarmServiceFromActivity() {

        var activity = MainActivity()

        // set current time to Monday, 24 February 2020 10:30:10.150 GMT +2:00
        var calendar = Calendar.getInstance()
        calendar.isLenient = true
        calendar.timeInMillis = 1582533010000
        calendar.set(Calendar.MILLISECOND, 150)
        var currentMillis = calendar.timeInMillis

        //--------------------------------------------------------
        // AlarmPeriod is currently active

        // 10:29 - 11:00
        var activePeriod = AlarmPeriod(629, 660)
        activePeriod.enabled = true
        mTestRealm.executeTransaction {
            it.insert(activePeriod)
        }
        assertEquals(0, Utils.getNextAlarmMillis(mTestRealm, currentMillis))
        mTestRealm.executeTransaction {
            it.deleteAll()
        }

        //--------------------------------------------------------
        // AlarmPeriod starts later

        // 10:33 - 11:00
        var futurePeriod = AlarmPeriod(633, 660)
        futurePeriod.enabled = true
        mTestRealm.executeTransaction {
            it.insert(futurePeriod)
        }
        calendar.set(Calendar.HOUR_OF_DAY, 10)
        calendar.set(Calendar.MINUTE, 33)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        var expectedTime = calendar.timeInMillis
        assertEquals(expectedTime, Utils.getNextAlarmMillis(mTestRealm, currentMillis))
        mTestRealm.executeTransaction {
            it.deleteAll()
        }

        //--------------------------------------------------------
        // AlarmPeriod starts right before midnight

        // 23:58 - 01:00
        futurePeriod = AlarmPeriod(1438, 60)
        futurePeriod.enabled = true
        mTestRealm.executeTransaction {
            it.insert(futurePeriod)
        }
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 58)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        expectedTime = calendar.timeInMillis
        assertEquals(expectedTime, Utils.getNextAlarmMillis(mTestRealm, currentMillis))
        mTestRealm.executeTransaction {
            it.deleteAll()
        }

        //--------------------------------------------------------
        // Only inactive periods

        var inactivePeriod = AlarmPeriod(334, 540)
        inactivePeriod.enabled = false
        var inactivePeriod2 = AlarmPeriod(1000, 1200)
        inactivePeriod.enabled = false
        mTestRealm.executeTransaction {
            it.insert(inactivePeriod)
            it.insert(inactivePeriod2)
        }
        assertEquals(-1, Utils.getNextAlarmMillis(mTestRealm, currentMillis))
        mTestRealm.executeTransaction {
            it.deleteAll()
        }

        //--------------------------------------------------------
        // Multiple AlarmPeriods

        // 23:58 - 01:00
        futurePeriod = AlarmPeriod(1438, 60)
        futurePeriod.enabled = true
        var futurePeriod2 = AlarmPeriod(1200, 1438)
        futurePeriod2.enabled = true
        mTestRealm.executeTransaction {
            it.insert(futurePeriod)
            it.insert(futurePeriod2)
        }
        calendar.set(Calendar.HOUR_OF_DAY, 20)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        expectedTime = calendar.timeInMillis
        assertEquals(expectedTime, Utils.getNextAlarmMillis(mTestRealm, currentMillis))
        mTestRealm.executeTransaction {
            it.deleteAll()
        }

        //--------------------------------------------------------
        // AlarmPeriod starts after midnight

        // 05:00 - 08:20
        futurePeriod = AlarmPeriod(300, 500)
        futurePeriod.enabled = true
        mTestRealm.executeTransaction {
            it.insert(futurePeriod)
        }
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 5)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        expectedTime = calendar.timeInMillis
        assertEquals(expectedTime, Utils.getNextAlarmMillis(mTestRealm, currentMillis))
        mTestRealm.executeTransaction {
            it.deleteAll()
        }

    }

    /**
     * Tests that AlarmService can schedule end time correctly (when no more alarms are active).
     */
    @Test
    fun scheduleAlarmServiceEnd() {
        var alarmService = AlarmService()

        // set current time to Monday, 24 February 2020 10:30:10.150 GMT +2:00
        var calendar = Calendar.getInstance()
        calendar.isLenient = true
        calendar.timeInMillis = 1582533010000
        calendar.set(Calendar.MILLISECOND, 150)
        var currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        var currentMinutes = calendar.get(Calendar.MINUTE)
        var currentSeconds = calendar.get(Calendar.SECOND)
        var currentMillis = calendar.get(Calendar.MILLISECOND)
        var currentTimeMillis = calendar.timeInMillis

        //--------------------------------------------------------
        // No active periods

        // 10:29 - 11:00
        var inactivePeriod = AlarmPeriod(629, 660)
        inactivePeriod.enabled = false
        mTestRealm.executeTransaction {
            it.insert(inactivePeriod)
        }
        assertEquals(0, alarmService.getTotalAlarmMillis(mTestRealm, currentTimeMillis))
        mTestRealm.executeTransaction {
            it.deleteAll()
        }

        //--------------------------------------------------------
        // One active period

        // 10:29 - 11:00
        var activePeriod = AlarmPeriod(629, 660)
        activePeriod.enabled = true
        mTestRealm.executeTransaction {
            it.insert(activePeriod)
        }
        var expectedLength = (activePeriod.endMinutes*60*1000L) - (currentMillis + (1000L*currentSeconds)
        + (1000L*60*currentMinutes) + (1000L*60*60*currentHour))
        assertEquals(expectedLength, alarmService.getTotalAlarmMillis(mTestRealm, currentTimeMillis))
        mTestRealm.executeTransaction {
            it.deleteAll()
        }

        //--------------------------------------------------------
        // Overlapping active periods

        // 10:29 - 11:00
        activePeriod = AlarmPeriod(629, 660)
        var activePeriod2 = AlarmPeriod(655, 713)
        activePeriod.enabled = true
        activePeriod2.enabled = true
        mTestRealm.executeTransaction {
            it.insert(activePeriod)
            it.insert(activePeriod2)
        }
        expectedLength = (activePeriod2.endMinutes*60*1000L) - (currentMillis + (1000L*currentSeconds)
                + (1000L*60*currentMinutes) + (1000L*60*60*currentHour))
        assertEquals(expectedLength, alarmService.getTotalAlarmMillis(mTestRealm, currentTimeMillis))
        mTestRealm.executeTransaction {
            it.deleteAll()
        }

        //--------------------------------------------------------
        // Active period over midnight

        // 10:29 - 01:40
        activePeriod = AlarmPeriod(629, 100)
        activePeriod.enabled = true
        mTestRealm.executeTransaction {
            it.insert(activePeriod)
        }
        expectedLength = ((activePeriod.endMinutes+1440)*60*1000L) - (currentMillis + (1000L*currentSeconds)
                + (1000L*60*currentMinutes) + (1000L*60*60*currentHour))
        assertEquals(expectedLength, alarmService.getTotalAlarmMillis(mTestRealm, currentTimeMillis))
        mTestRealm.executeTransaction {
            it.deleteAll()
        }

        //--------------------------------------------------------
        // Another period starting later

        // 10:29 - 13:20
        activePeriod = AlarmPeriod(629, 800)
        // 01:00 - 08:20
        activePeriod2 = AlarmPeriod(60, 500)
        activePeriod.enabled = true
        activePeriod2.enabled = true
        mTestRealm.executeTransaction {
            it.insert(activePeriod)
            it.insert(activePeriod2)
        }
        expectedLength = ((activePeriod.endMinutes)*60*1000L) - (currentMillis + (1000L*currentSeconds)
                + (1000L*60*currentMinutes) + (1000L*60*60*currentHour))
        assertEquals(expectedLength, alarmService.getTotalAlarmMillis(mTestRealm, currentTimeMillis))
        mTestRealm.executeTransaction {
            it.deleteAll()
        }

    } // @test



    /**
     * Tests that AlarmService can schedule itself to start again when alarm period is over.
     */
    @Test
    fun scheduleAlarmServiceRestart() {
        var alarmService = AlarmService()

        // set current time to Monday, 24 February 2020 10:30:10.150 GMT +2:00
        var calendar = Calendar.getInstance()
        calendar.isLenient = true
        calendar.timeInMillis = 1582533010000
        calendar.set(Calendar.MILLISECOND, 150)
        var currentMillis = calendar.timeInMillis

        //--------------------------------------------------------
        // No active periods

        // 10:29 - 11:00
        var inactivePeriod = AlarmPeriod(629, 660)
        inactivePeriod.enabled = false
        mTestRealm.executeTransaction {
            it.insert(inactivePeriod)
        }
        assertEquals(0, alarmService.getTotalAlarmMillis(mTestRealm, currentMillis))
        mTestRealm.executeTransaction {
            it.deleteAll()
        }



    }
}