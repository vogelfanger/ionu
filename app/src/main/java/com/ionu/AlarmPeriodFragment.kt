package com.ionu

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import io.realm.Realm

class AlarmPeriodFragment: Fragment() {

    private lateinit var mSwitch : Switch
    private lateinit var mStartTime : TextView
    private lateinit var mEndTime : TextView
    private lateinit var mMessageEditor : EditText
    private lateinit var mAlarmPeriod : AlarmPeriod
    private lateinit var mRealm : Realm

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_alarm_period, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var alarmID : String = ""
        if(savedInstanceState != null) {
            //TODO change key to global variable
            alarmID = savedInstanceState.getString("alarm_period_id_bundle")
        }
        mRealm = Realm.getDefaultInstance()
        mRealm.executeTransaction {
            val managedAlarm = it.where(AlarmPeriod::class.java).equalTo("id", alarmID).findFirst()
            if(managedAlarm != null){
                mAlarmPeriod = managedAlarm
            }else {
                Log.e("AlarmPeriodFragment", "Could not find alarm from Realm")
                // error state, changes made to this object will not be saved to Realm
                mAlarmPeriod = AlarmPeriod()
            }
        }

        mSwitch = view.findViewById(R.id.alarm_enabled_switch)
        mStartTime = view.findViewById(R.id.alarm_start_time_interaction_view)
        mEndTime = view.findViewById(R.id.alarm_end_time_interaction_view)
        mMessageEditor = view.findViewById(R.id.alarm_custom_message_edit_text)

        mSwitch.setOnCheckedChangeListener { switchView, isChecked ->
            if(isChecked) {
                // TODO activate alarm (in Activity or Service)
                mAlarmPeriod.enabled = isChecked
            } else{
                // TODO disable alarm (in Activity or Service)
                mAlarmPeriod.enabled = isChecked
            }
        }

        mStartTime.setOnClickListener{
            // TODO open time selection view
        }

        mEndTime.setOnClickListener{
            // TODO open time selection view
        }

        // TODO figure out how to manage EditText, maybe just save the string on focus change
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mRealm.close()
    }
}