package com.ionu


import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
lateinit var mWeekFailuresView : TextView
lateinit var mWeekTotalTimeView : TextView
lateinit var mWeekBestTimeView : TextView
lateinit var mMonthFailuresView : TextView
lateinit var mMonthTotalTimeView : TextView
lateinit var mMonthBestTimeView : TextView
lateinit var mCurrentStreakView : TextView
lateinit var mBestStreakView : TextView
lateinit var mAlltimeTotalTimeView : TextView

class HistoryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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




    fun updateView(context : Context) {
        Log.d("HistoryFragment", "updateView()")

        // get data from shared prefs, service updates those after each period
        // so that they don't need to be calculated here every time fragment is shown
        var prefs = PreferenceManager.getDefaultSharedPreferences(context)

        // Set week failures
        mWeekFailuresView.text = prefs.getLong(GlobalVariables.PREF_KEY_WEEKLY_FAILURES, 0L).toString()

        // Set current week total
        Log.d("HistoryFragment", "current week in prefs " + prefs.getLong(GlobalVariables.PREF_KEY_CURRENT_WEEK, 0L))
        var prefMinutes = prefs.getLong(GlobalVariables.PREF_KEY_CURRENT_WEEK, 0L) / 60000
        var prefHours = prefMinutes / 60L
        // set remaining minutes left over from hours
        prefMinutes = prefMinutes - (prefHours * 60L)
        mWeekTotalTimeView.text = "" + prefHours + "h " + prefMinutes + "min"

        // Set best week
        Log.d("HistoryFragment", "best week in prefs " + prefs.getLong(GlobalVariables.PREF_KEY_BEST_WEEK, 0L))
        prefMinutes = prefs.getLong(GlobalVariables.PREF_KEY_BEST_WEEK, 0L) / 60000L
        prefHours = prefMinutes / 60L
        prefMinutes = prefMinutes - (prefHours * 60L)
        mWeekBestTimeView.setText("" + prefHours + "h " + prefMinutes + "min")

        // Set month failures
        mMonthFailuresView.text = prefs.getLong(GlobalVariables.PREF_KEY_MONTHLY_FAILURES, 0L).toString()

        // Set current month
        Log.d("HistoryFragment", "current month in prefs " + prefs.getLong(GlobalVariables.PREF_KEY_CURRENT_MONTH, 0L))
        prefMinutes = prefs.getLong(GlobalVariables.PREF_KEY_CURRENT_MONTH, 0L) / 60000
        prefHours = prefMinutes / 60L
        prefMinutes = prefMinutes - (prefHours * 60L)
        mMonthTotalTimeView.setText("" + prefHours + "h " + prefMinutes + "min")

        // Set best month
        Log.d("HistoryFragment", "best month in prefs " + prefs.getLong(GlobalVariables.PREF_KEY_BEST_MONTH, 0L))
        prefMinutes = prefs.getLong(GlobalVariables.PREF_KEY_BEST_MONTH, 0L) / 60000
        prefHours = prefMinutes / 60L
        prefMinutes = prefMinutes - (prefHours * 60L)
        mMonthBestTimeView.setText("" + prefHours + "h " + prefMinutes + "min")

        // Set current streak
        prefMinutes = prefs.getLong(GlobalVariables.PREF_KEY_CURRENT_STREAK, 0L) / 60000
        prefHours = prefMinutes / 60L
        prefMinutes = prefMinutes - (prefHours * 60L)
        mCurrentStreakView.setText("" + prefHours + "h " + prefMinutes + "min")

        // Set best streak
        prefMinutes = prefs.getLong(GlobalVariables.PREF_KEY_BEST_STREAK, 0L) / 60000
        prefHours = prefMinutes / 60L
        prefMinutes = prefMinutes - (prefHours * 60L)
        mBestStreakView.setText("" + prefHours + "h " + prefMinutes + "min")

        // Set alltime total
        prefMinutes = prefs.getLong(GlobalVariables.PREF_KEY_ALLTIME_TOTAL_TIME, 0L) / 60000
        prefHours = prefMinutes / 60L
        prefMinutes = prefMinutes - (prefHours * 60L)
        mAlltimeTotalTimeView.setText("" + prefHours + "h " + prefMinutes + "min")
    }
}
