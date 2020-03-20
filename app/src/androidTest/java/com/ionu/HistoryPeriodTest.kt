package com.ionu

import android.content.Context
import android.content.SharedPreferences
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import io.realm.Realm
import io.realm.RealmConfiguration
import org.junit.Assert

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import java.util.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class HistoryPeriodTest {

    lateinit var mTestRealm: Realm
    lateinit var mPrefs: SharedPreferences
    lateinit var mContext: Context
    val PREF_TAG = "HistoryPeriodTest"

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


    @Test
    fun updateAlltimeDataInService() {

        Assert.assertEquals(0L, mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_STREAK, 0L))
        Assert.assertEquals(0L, mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_STREAK, 0L))

        var alarmService = AlarmService()

        //-----------------------------------------------------------------------------
        // Failed period

        // Thursday, 20 February 2020 10:00:00 GMT to Thursday, 20 February 2020 11:00:00 GMT
        var firstPeriod = HistoryPeriod(1582192800000L, 1582196400000L, false)
        var totalTime = firstPeriod.getLenght()

        mTestRealm.executeTransaction {
            it.insert(firstPeriod)
        }
        alarmService.updateAlltimeData(mPrefs, mTestRealm)

        Assert.assertEquals(0L, mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_STREAK, 0L))
        Assert.assertEquals(0L, mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_STREAK, 0L))
        Assert.assertEquals(totalTime, mPrefs.getLong(GlobalVariables.PREF_KEY_ALLTIME_TOTAL_TIME, 0L))

        //-----------------------------------------------------------------------------
        // Successful period

        // Friday, 21 February 2020 16:00:00 to Friday, 21 February 2020 17:30:00
        var secondPeriod = HistoryPeriod(1582300800000L, 1582306200000L, true)
        totalTime += secondPeriod.getLenght()

        mTestRealm.executeTransaction {
            it.insert(secondPeriod)
        }
        alarmService.updateAlltimeData(mPrefs, mTestRealm)

        Assert.assertEquals(secondPeriod.getLenght(), mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_STREAK, 0L))
        Assert.assertEquals(secondPeriod.getLenght(), mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_STREAK, 0L))
        Assert.assertEquals(totalTime, mPrefs.getLong(GlobalVariables.PREF_KEY_ALLTIME_TOTAL_TIME, 0L))

        //-----------------------------------------------------------------------------
        // Another successful period

        // TUE, 25 Feb 2020 15:00:00 GMT to TUE, 25 Feb 2020 15:30:00 GMT
        var thirdPeriod = HistoryPeriod(1582642800000L, 1582644600000L, true)
        totalTime += thirdPeriod.getLenght()

        mTestRealm.executeTransaction {
            it.insert(thirdPeriod)
        }
        alarmService.updateAlltimeData(mPrefs, mTestRealm)

        Assert.assertEquals(secondPeriod.getLenght() + thirdPeriod.getLenght()
            , mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_STREAK, 0L))
        Assert.assertEquals(secondPeriod.getLenght() + thirdPeriod.getLenght()
            , mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_STREAK, 0L))
        Assert.assertEquals(totalTime, mPrefs.getLong(GlobalVariables.PREF_KEY_ALLTIME_TOTAL_TIME, 0L))

        //-----------------------------------------------------------------------------
        // Failure after success

        // Friday, 6 March 2020 05:00:00 GMT to Friday, 6 March 2020 10:00:00
        var fourthPeriod = HistoryPeriod(1583470800000L, 1583488800000L, false)
        totalTime += fourthPeriod.getLenght()

        mTestRealm.executeTransaction {
            it.insert(fourthPeriod)
        }
        alarmService.updateAlltimeData(mPrefs, mTestRealm)

        Assert.assertEquals(0L, mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_STREAK, 0L))
        Assert.assertEquals(secondPeriod.getLenght() + thirdPeriod.getLenght()
            , mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_STREAK, 0L))
        Assert.assertEquals(totalTime, mPrefs.getLong(GlobalVariables.PREF_KEY_ALLTIME_TOTAL_TIME, 0L))

        //-----------------------------------------------------------------------------
        // New streak, not best yet

        // Friday, 20 March 2020 13:00:00 GMT to Friday, 20 March 2020 13:50:00
        var fifthPeriod = HistoryPeriod(1584709200000L, 1584712200000L, true)
        totalTime += fifthPeriod.getLenght()

        mTestRealm.executeTransaction {
            it.insert(fifthPeriod)
        }
        alarmService.updateAlltimeData(mPrefs, mTestRealm)

        Assert.assertEquals(fifthPeriod.getLenght(), mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_STREAK, 0L))
        Assert.assertEquals(secondPeriod.getLenght() + thirdPeriod.getLenght()
            , mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_STREAK, 0L))
        Assert.assertEquals(totalTime, mPrefs.getLong(GlobalVariables.PREF_KEY_ALLTIME_TOTAL_TIME, 0L))

        //-----------------------------------------------------------------------------
        // Streak becomes new best

        // Monday, 23 March 2020 07:00:00 GMT to Monday, 23 March 2020 20:00:00
        var sixthPeriod = HistoryPeriod(1584946800000L, 1584993600000L, true)
        totalTime += sixthPeriod.getLenght()

        mTestRealm.executeTransaction {
            it.insert(sixthPeriod)
        }
        alarmService.updateAlltimeData(mPrefs, mTestRealm)

        Assert.assertEquals(fifthPeriod.getLenght() + sixthPeriod.getLenght(),
            mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_STREAK, 0L))
        Assert.assertEquals(fifthPeriod.getLenght() + sixthPeriod.getLenght(),
            mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_STREAK, 0L))
        Assert.assertEquals(totalTime, mPrefs.getLong(GlobalVariables.PREF_KEY_ALLTIME_TOTAL_TIME, 0L))

    }





    @Test
    fun updateWeeklyDataInService() {

        Assert.assertEquals(0L, mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_WEEK, 0L))
        Assert.assertEquals(0L, mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_WEEK, 0L))

        var alarmService = AlarmService()
        var calendar = Calendar.getInstance()

        //-----------------------------------------------------------------------------
        // One period gets saved this week

        // Thursday, 20 February 2020 10:00:00 GMT to Thursday, 20 February 2020 11:00:00 GMT
        var firstPeriod = HistoryPeriod(1582192800000L, 1582196400000L, false)
        mTestRealm.executeTransaction {
            it.insert(firstPeriod)
        }

        // Sunday 23 February 2020 20:00:00 GMT
        calendar.timeInMillis = 1582488000000L
        alarmService.updateWeeklyData(mPrefs, calendar, mTestRealm)
        Assert.assertEquals(firstPeriod.getLenght(), mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_WEEK, 0L))
        Assert.assertEquals(firstPeriod.getLenght(), mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_WEEK, 0L))

        //-----------------------------------------------------------------------------
        // Another period is saved this week

        // Friday, 21 February 2020 16:00:00 to Friday, 21 February 2020 17:30:00
        var secondPeriod = HistoryPeriod(1582300800000L, 1582306200000L, false)
        mTestRealm.executeTransaction {
            it.insert(secondPeriod)
        }
        alarmService.updateWeeklyData(mPrefs, calendar, mTestRealm)
        Assert.assertEquals(firstPeriod.getLenght() + secondPeriod.getLenght(),
            mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_WEEK, 0L))
        Assert.assertEquals(firstPeriod.getLenght() + secondPeriod.getLenght(),
            mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_WEEK, 0L))

        //-----------------------------------------------------------------------------
        // Another period is saved next week, previous week is still best

        // Sunday 1 March 2020 20:00:00 GMT
        calendar.timeInMillis = 1583092800000L

        // TUE, 25 Feb 2020 15:00:00 GMT to TUE, 25 Feb 2020 15:30:00 GMT
        var thirdPeriod = HistoryPeriod(1582642800000L, 1582644600000L, true)
        mTestRealm.executeTransaction {
            it.insert(thirdPeriod)
        }
        alarmService.updateWeeklyData(mPrefs, calendar, mTestRealm)
        Assert.assertEquals(thirdPeriod.getLenght(), mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_WEEK, 0L))
        Assert.assertEquals(firstPeriod.getLenght() + secondPeriod.getLenght(),
            mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_WEEK, 0L))

        //-----------------------------------------------------------------------------
        // Another period is saved the week after, best week changes

        // Sunday, 8 March 2020 20:00:00 GMT
        calendar.timeInMillis = 1583697600000L

        // Friday, 6 March 2020 05:00:00 GMT to Friday, 6 March 2020 10:00:00
        var fourthPeriod = HistoryPeriod(1583470800000L, 1583488800000L, true)
        mTestRealm.executeTransaction {
            it.insert(fourthPeriod)
        }
        alarmService.updateWeeklyData(mPrefs, calendar, mTestRealm)
        Assert.assertEquals(fourthPeriod.getLenght(), mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_WEEK, 0L))
        Assert.assertEquals(fourthPeriod.getLenght(), mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_WEEK, 0L))

        //-----------------------------------------------------------------------------
        // Another period is saved, but in between weeks, next week becomes new best

        // 15 March 2020 20:00:00 GMT
        calendar.timeInMillis = 1584302400000L

        // get start of this week in milliseconds
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.clear(Calendar.MINUTE)
        calendar.clear(Calendar.SECOND)
        calendar.clear(Calendar.MILLISECOND)
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        var startOfWeek = calendar.timeInMillis
        calendar.timeInMillis = 1584302400000L

        // Sunday, 8 March 2020 21:00:00 GMT to Monday, 9 March 2020 17:00:00
        var fifthPeriod = HistoryPeriod(1583701200000L, 1583773200000L, true)
        mTestRealm.executeTransaction {
            it.insert(fifthPeriod)
        }
        alarmService.updateWeeklyData(mPrefs, calendar, mTestRealm)

        var nextWeekPeriodLength = fifthPeriod.endMillis - startOfWeek
        Assert.assertEquals(nextWeekPeriodLength, mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_WEEK, 0L))
        Assert.assertEquals(nextWeekPeriodLength, mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_WEEK, 0L))

        //-----------------------------------------------------------------------------
        // Another period is saved, but in between weeks, previous week was best

        var lastWeekBest = mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_WEEK, 0L)

        // Sunday, 22 March 2020 20:00:00 GMT
        calendar.timeInMillis = 1584907200000L

        // get start of this week in milliseconds
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.clear(Calendar.MINUTE)
        calendar.clear(Calendar.SECOND)
        calendar.clear(Calendar.MILLISECOND)
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        startOfWeek = calendar.timeInMillis
        calendar.timeInMillis = 1584907200000L

        // Sunday, 15 March 2020 20:00:00 GMT to Monday, 16 March 2020 04:00:00 GMT
        var sixthPeriod = HistoryPeriod(1584302400000L, 1584331200000L, true)
        mTestRealm.executeTransaction {
            it.insert(sixthPeriod)
        }
        alarmService.updateWeeklyData(mPrefs, calendar, mTestRealm)
        // Monday, 16 March 2020 00:00:00, start of week
        nextWeekPeriodLength = sixthPeriod.endMillis - startOfWeek
        lastWeekBest = lastWeekBest + (sixthPeriod.getLenght() - nextWeekPeriodLength)
        Assert.assertEquals(nextWeekPeriodLength, mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_WEEK, 0L))
        // test if previous week's best was updated properly
        Assert.assertEquals(lastWeekBest, mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_WEEK, 0L))

        //-----------------------------------------------------------------------------
        // Another period is saved the same week

        var lastCurrent = mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_WEEK, 0L)
        lastWeekBest = mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_WEEK, 0L)

        // Wednesday, 18 March 2020 05:00:00 GMT to Wednesday, 18 March 2020 13:00:00
        var seventhPeriod = HistoryPeriod(1584507600000L, 1584536400000L, true)
        mTestRealm.executeTransaction {
            it.insert(seventhPeriod)
        }
        alarmService.updateWeeklyData(mPrefs, calendar, mTestRealm)
        Assert.assertEquals(lastCurrent + seventhPeriod.getLenght(), mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_WEEK, 0L))
        Assert.assertEquals(lastWeekBest, mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_WEEK, 0L))
    }





    @Test
    fun updateMonthlyDataInService() {

        Assert.assertEquals(0L, mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_MONTH, 0L))
        Assert.assertEquals(0L, mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_MONTH, 0L))

        var alarmService = AlarmService()
        var calendar = Calendar.getInstance()

        //-----------------------------------------------------------------------------
        // One period gets saved this month

        // Thursday, 20 February 2020 10:00:00 GMT to Thursday, 20 February 2020 11:00:00 GMT
        var firstPeriod = HistoryPeriod(1582192800000L, 1582196400000L, false)
        mTestRealm.executeTransaction {
            it.insert(firstPeriod)
        }

        // Sunday 23 February 2020 20:00:00 GMT
        calendar.timeInMillis = 1582488000000L
        alarmService.updateMonthlyData(mPrefs, calendar, mTestRealm)
        Assert.assertEquals(firstPeriod.getLenght(), mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_MONTH, 0L))
        Assert.assertEquals(firstPeriod.getLenght(), mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_MONTH, 0L))

        //-----------------------------------------------------------------------------
        // Another period is saved this month

        // Friday, 21 February 2020 16:00:00 to Friday, 21 February 2020 17:30:00
        var secondPeriod = HistoryPeriod(1582300800000L, 1582306200000L, false)
        mTestRealm.executeTransaction {
            it.insert(secondPeriod)
        }
        alarmService.updateMonthlyData(mPrefs, calendar, mTestRealm)
        Assert.assertEquals(firstPeriod.getLenght() + secondPeriod.getLenght(),
            mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_MONTH, 0L))
        Assert.assertEquals(firstPeriod.getLenght() + secondPeriod.getLenght(),
            mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_MONTH, 0L))

        //-----------------------------------------------------------------------------
        // Another period is saved next month, previous month is still best

        //  Sunday, 15 March 2020 20:00:00 GMT
        calendar.timeInMillis = 1584302400000L

        // Wednesday, 4 March 2020 06:00:00 GMT to Wednesday, 4 March 2020 07:00:00 GMT
        var thirdPeriod = HistoryPeriod(1583301600000L, 1583305200000L, true)
        mTestRealm.executeTransaction {
            it.insert(thirdPeriod)
        }
        alarmService.updateMonthlyData(mPrefs, calendar, mTestRealm)
        Assert.assertEquals(thirdPeriod.getLenght(), mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_MONTH, 0L))
        Assert.assertEquals(firstPeriod.getLenght() + secondPeriod.getLenght(),
            mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_MONTH, 0L))

        //-----------------------------------------------------------------------------
        // Another period is saved the month after, best month changes

        // Saturday, 25 April 2020 20:00:00 GMT
        calendar.timeInMillis = 1587844800000L

        // Friday, 17 April 2020 13:00:00 GMT to Friday, 17 April 2020 18:00:00
        var fourthPeriod = HistoryPeriod(1587128400000L, 1587146400000L, true)
        mTestRealm.executeTransaction {
            it.insert(fourthPeriod)
        }
        alarmService.updateMonthlyData(mPrefs, calendar, mTestRealm)
        Assert.assertEquals(fourthPeriod.getLenght(), mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_MONTH, 0L))
        Assert.assertEquals(fourthPeriod.getLenght(), mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_MONTH, 0L))

        //-----------------------------------------------------------------------------
        // Another period is saved, but in between months, next month becomes new best

        // Sunday, 17 May 2020 18:00:00 GMT
        calendar.timeInMillis = 1589738400000L

        // get start of this month in milliseconds
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.clear(Calendar.MINUTE)
        calendar.clear(Calendar.SECOND)
        calendar.clear(Calendar.MILLISECOND)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        var startOfMonth = calendar.timeInMillis
        calendar.timeInMillis = 1589738400000L

        // Thursday, 30 April 2020 20:00:00 GMT to Friday, 1 May 2020 17:00:00
        var fifthPeriod = HistoryPeriod(1588276800000L, 1588352400000L, true)
        mTestRealm.executeTransaction {
            it.insert(fifthPeriod)
        }
        alarmService.updateMonthlyData(mPrefs, calendar, mTestRealm)

        var nextMonthPeriodLength = fifthPeriod.endMillis - startOfMonth
        Assert.assertEquals(nextMonthPeriodLength, mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_MONTH, 0L))
        Assert.assertEquals(nextMonthPeriodLength, mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_MONTH, 0L))

        //-----------------------------------------------------------------------------
        // Another period is saved, but in between months, previous month was best

        var lastMonthBest = mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_MONTH, 0L)

        // Thursday, 25 June 2020 20:00:00 GMT
        calendar.timeInMillis = 1593115200000L

        // get start of this month in milliseconds
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.clear(Calendar.MINUTE)
        calendar.clear(Calendar.SECOND)
        calendar.clear(Calendar.MILLISECOND)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        startOfMonth = calendar.timeInMillis
        calendar.timeInMillis = 1593115200000L

        // Sunday, 31 May 2020 20:00:00 GMT to Monday, 1 June 2020 05:00:00 GMT
        var sixthPeriod = HistoryPeriod(1590955200000L, 1590987600000L, true)
        mTestRealm.executeTransaction {
            it.insert(sixthPeriod)
        }
        alarmService.updateMonthlyData(mPrefs, calendar, mTestRealm)
        nextMonthPeriodLength = sixthPeriod.endMillis - startOfMonth
        lastMonthBest = lastMonthBest + (sixthPeriod.getLenght() - nextMonthPeriodLength)
        Assert.assertEquals(nextMonthPeriodLength, mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_MONTH, 0L))
        // test if previous month's best was updated properly
        Assert.assertEquals(lastMonthBest, mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_MONTH, 0L))

        //-----------------------------------------------------------------------------
        // Another period is saved the same month

        var lastCurrent = mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_MONTH, 0L)
        lastMonthBest = mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_MONTH, 0L)

        // Saturday, 6 June 2020 12:00:00 GMT to  Saturday, 6 June 2020 16:00:00
        var seventhPeriod = HistoryPeriod(1591444800000L, 1591459200000L, true)
        mTestRealm.executeTransaction {
            it.insert(seventhPeriod)
        }
        alarmService.updateMonthlyData(mPrefs, calendar, mTestRealm)
        Assert.assertEquals(lastCurrent + seventhPeriod.getLenght(), mPrefs.getLong(GlobalVariables.PREF_KEY_CURRENT_MONTH, 0L))
        Assert.assertEquals(lastMonthBest, mPrefs.getLong(GlobalVariables.PREF_KEY_BEST_MONTH, 0L))
    }

}

