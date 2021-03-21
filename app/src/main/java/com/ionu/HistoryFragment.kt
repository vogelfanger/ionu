package com.ionu


import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * Fragment that displays history data.
 */
class HistoryFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener{

    private lateinit var mWeekFailuresView : TextView
    private lateinit var mWeekTotalTimeView : TextView
    private lateinit var mWeekBestTimeView : TextView
    private lateinit var mMonthFailuresView : TextView
    private lateinit var mMonthTotalTimeView : TextView
    private lateinit var mMonthBestTimeView : TextView
    private lateinit var mCurrentStreakView : TextView
    private lateinit var mBestStreakView : TextView
    private lateinit var mAlltimeTotalTimeView : TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mWeekFailuresView = view.findViewById(R.id.card_text_value_weekly_ratio)
        mWeekTotalTimeView = view.findViewById(R.id.card_text_value_weekly_total)
        mWeekBestTimeView = view.findViewById(R.id.card_text_value_best_week)
        mMonthFailuresView = view.findViewById(R.id.card_text_value_monthly_ratio)
        mMonthTotalTimeView = view.findViewById(R.id.card_text_value_monthly_total)
        mMonthBestTimeView = view.findViewById(R.id.card_text_value_best_month)
        mCurrentStreakView = view.findViewById(R.id.card_text_value_current_streak)
        mBestStreakView = view.findViewById(R.id.card_text_value_best_streak)
        mAlltimeTotalTimeView = view.findViewById(R.id.card_text_value_alltime_total_time)

        if(activity != null) {
            var parent = activity as AppCompatActivity
            updateView(parent.applicationContext)
        }
    }

    override fun onResume() {
        super.onResume()
        if(activity != null) {
            var parent = activity as AppCompatActivity
            updateView(parent.applicationContext)
        }
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {

        if(p1.equals(GlobalVariables.PREF_KEY_ALLTIME_TOTAL_TIME) ||
            p1.equals(GlobalVariables.PREF_KEY_MONTHLY_FAILURES) ||
            p1.equals(GlobalVariables.PREF_KEY_WEEKLY_FAILURES) ||
            p1.equals(GlobalVariables.PREF_KEY_CURRENT_MONTH) ||
            p1.equals(GlobalVariables.PREF_KEY_CURRENT_WEEK) ||
            p1.equals(GlobalVariables.PREF_KEY_CURRENT_STREAK) ||
            p1.equals(GlobalVariables.PREF_KEY_BEST_MONTH) ||
            p1.equals(GlobalVariables.PREF_KEY_BEST_WEEK) ||
            p1.equals(GlobalVariables.PREF_KEY_BEST_STREAK)) {

            if(activity != null) {
                var parent = activity as AppCompatActivity
                updateView(parent.applicationContext)
            }
        }
    }

    /**
     * Updates view with data found from shared preferences.
     */
    fun updateView(context : Context) {

        Log.d("HistoryFragment", "updateView()")

        if(activity != null) {
            var parent = activity as AppCompatActivity

            // check if a view is initialized, if it is then the other ones are probably too
            if (this::mWeekFailuresView.isInitialized) {

                // get data from shared prefs, service updates those after each period
                // so that they don't need to be calculated here every time fragment is shown
                var prefs = PreferenceManager.getDefaultSharedPreferences(context)

                // Set week failures
                parent.runOnUiThread {
                    mWeekFailuresView.text = prefs.getLong(GlobalVariables.PREF_KEY_WEEKLY_FAILURES, 0L).toString()
                }

                // Set current week total
                Log.d(
                    "HistoryFragment",
                    "current week in prefs " + prefs.getLong(GlobalVariables.PREF_KEY_CURRENT_WEEK, 0L)
                )
                var prefMinutes = prefs.getLong(GlobalVariables.PREF_KEY_CURRENT_WEEK, 0L) / 60000
                var prefHours = prefMinutes / 60L
                // set remaining minutes left over from hours
                prefMinutes = prefMinutes - (prefHours * 60L)
                parent.runOnUiThread {
                    mWeekTotalTimeView.text = "" + prefHours + "h " + prefMinutes + "min"
                }


                // Set best week
                Log.d("HistoryFragment", "best week in prefs " + prefs.getLong(GlobalVariables.PREF_KEY_BEST_WEEK, 0L))
                prefMinutes = prefs.getLong(GlobalVariables.PREF_KEY_BEST_WEEK, 0L) / 60000L
                prefHours = prefMinutes / 60L
                prefMinutes = prefMinutes - (prefHours * 60L)
                parent.runOnUiThread {
                    mWeekBestTimeView.text = "" + prefHours + "h " + prefMinutes + "min"
                }


                // Set month failures
                parent.runOnUiThread {
                    mMonthFailuresView.text = prefs.getLong(GlobalVariables.PREF_KEY_MONTHLY_FAILURES, 0L).toString()
                }

                // Set current month
                Log.d(
                    "HistoryFragment",
                    "current month in prefs " + prefs.getLong(GlobalVariables.PREF_KEY_CURRENT_MONTH, 0L)
                )
                prefMinutes = prefs.getLong(GlobalVariables.PREF_KEY_CURRENT_MONTH, 0L) / 60000
                prefHours = prefMinutes / 60L
                prefMinutes = prefMinutes - (prefHours * 60L)
                parent.runOnUiThread {
                    mMonthTotalTimeView.setText("" + prefHours + "h " + prefMinutes + "min")
                }

                // Set best month
                Log.d("HistoryFragment", "best month in prefs " + prefs.getLong(GlobalVariables.PREF_KEY_BEST_MONTH, 0L))
                prefMinutes = prefs.getLong(GlobalVariables.PREF_KEY_BEST_MONTH, 0L) / 60000
                prefHours = prefMinutes / 60L
                prefMinutes = prefMinutes - (prefHours * 60L)
                parent.runOnUiThread {
                    mMonthBestTimeView.text = "" + prefHours + "h " + prefMinutes + "min"
                }

                // Set current streak
                prefMinutes = prefs.getLong(GlobalVariables.PREF_KEY_CURRENT_STREAK, 0L) / 60000
                prefHours = prefMinutes / 60L
                prefMinutes = prefMinutes - (prefHours * 60L)
                parent.runOnUiThread {
                    mCurrentStreakView.text = "" + prefHours + "h " + prefMinutes + "min"
                }


                // Set best streak
                prefMinutes = prefs.getLong(GlobalVariables.PREF_KEY_BEST_STREAK, 0L) / 60000
                prefHours = prefMinutes / 60L
                prefMinutes = prefMinutes - (prefHours * 60L)
                parent.runOnUiThread {
                    mBestStreakView.text = "" + prefHours + "h " + prefMinutes + "min"
                }

                // Set alltime total
                prefMinutes = prefs.getLong(GlobalVariables.PREF_KEY_ALLTIME_TOTAL_TIME, 0L) / 60000
                prefHours = prefMinutes / 60L
                prefMinutes = prefMinutes - (prefHours * 60L)
                parent.runOnUiThread {
                    mAlltimeTotalTimeView.text = "" + prefHours + "h " + prefMinutes + "min"
                }
            }
        }
    }
}
