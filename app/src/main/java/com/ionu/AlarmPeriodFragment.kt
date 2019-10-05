package com.ionu

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import io.realm.Realm

class AlarmPeriodFragment: Fragment(), TimePickerFragment.TimePickerListener {

    private lateinit var mSwitch : Switch
    private lateinit var mStartTime : TextView
    private lateinit var mEndTime : TextView
    private lateinit var mMessageEditor : EditText
    private lateinit var mRealm : Realm
    private var mAlarmID : String
    private var mAlarmPeriod : AlarmPeriod?

    val startTimePickerTag = "startTimePicker"
    val endTimePickerTag = "endTimePicker"

    init {
        mAlarmID = ""
        mAlarmPeriod = null
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_alarm_period, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle = this.arguments
        bundle?.let{
            mAlarmID = it.getString(GlobalVariables.ALARM_PERIOD_ID_BUNDLE)
            Log.d("AlarmPeriodFragment", "bundle found, id: " + mAlarmID)
        }

        mSwitch = view.findViewById(R.id.alarm_enabled_switch)
        mStartTime = view.findViewById(R.id.alarm_start_time_interaction_view)
        mEndTime = view.findViewById(R.id.alarm_end_time_interaction_view)
        mMessageEditor = view.findViewById(R.id.alarm_custom_message_edit_text)

        mRealm = Realm.getDefaultInstance()

        // get selected alarm from Realm and set initial values for widgets
        mRealm.executeTransaction {
            mAlarmPeriod = mRealm.where(AlarmPeriod::class.java).equalTo("id", mAlarmID).findFirst()
            mAlarmPeriod?.let{
                Log.d("AlarmPeriodFragment", "found alarm from Realm, id: " + it.id)
                mSwitch.isChecked = it.enabled
                mStartTime.text = it.startTimeAsString()
                mEndTime.text = it.endTimeAsString()
                mMessageEditor.setText(it.message)
            }
        }

        mSwitch.setOnCheckedChangeListener { switchView, isChecked ->
            mRealm.executeTransaction {
                mAlarmPeriod?.let{
                    it.enabled = isChecked
                    if(isChecked) {
                        // TODO activate alarm (in Activity or Service)
                        Log.d("AlarmPeriodFragment", "alarm enabled")
                    } else{
                        // TODO disable alarm (in Activity or Service)
                        Log.d("AlarmPeriodFragment", "alarm disabled")
                    }
                }
            }
        }

        mStartTime.setOnClickListener{
            TimePickerFragment().show(childFragmentManager, startTimePickerTag)
        }

        mEndTime.setOnClickListener{
            TimePickerFragment().show(childFragmentManager, endTimePickerTag)
        }

        // TODO needs to be tested, also might want to add onFocusChangeListener to make editing easier
        mMessageEditor.setOnEditorActionListener { view, actionId, event ->
            /**
            if(actionId == EditorInfo.IME_ACTION_DONE) {
            mRealm.executeTransaction {
            mAlarmPeriod?.let{
            it.message = view.text.toString()
            Log.d("AlarmPeriodFragment", "custom message saved: " + it.message)
            }
            }
            true
            }else false
             */
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    // save changes to Realm
                    mRealm.executeTransaction {
                        mAlarmPeriod?.let{
                            it.message = view.text.toString()
                            Log.d("AlarmPeriodFragment", "custom message saved: " + it.message)
                        }
                    }
                    // hide keyboard
                    context?.let {
                        val imm = it.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(view.windowToken, 0)
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mRealm.close()
    }

    override fun onPickerTimeSet(hours: Int, minutes: Int, fragmentTag: String) {
        Log.d("AlarmPeriodFragment", "onPickerTimeSet")
        mRealm.executeTransaction {
            mAlarmPeriod?.let{
                // use fragment tag to determine which picker was just set
                if(fragmentTag == startTimePickerTag){
                    Log.d("AlarmPeriodFragment", "start time picked")
                    it.startHours = hours
                    it.startMinutes = minutes
                    mStartTime.text = it.startTimeAsString()
                }
                else if(fragmentTag == endTimePickerTag) {
                    Log.d("AlarmPeriodFragment", "end time picked")
                    it.endHours = hours
                    it.endMinutes = minutes
                    mEndTime.text = it.endTimeAsString()
                }
            }
        }
    }
}