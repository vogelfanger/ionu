package com.ionu

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import io.realm.Realm

class AlarmPeriodFragment: Fragment(), TimePickerFragment.TimePickerListener {

    private lateinit var mSwitch : Switch
    private lateinit var mStartTime : TextView
    private lateinit var mEndTime : TextView
    private lateinit var mMessageEditor : EditText
    private lateinit var mDeleteButton : Button
    private lateinit var mRealm : Realm
    private var mAlarmID : String
    private var mAlarmPeriod : AlarmPeriod?
    private var mListener : AlarmPeriodFragmentListener? = null

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
        }

        mSwitch = view.findViewById(R.id.alarm_enabled_switch)
        mStartTime = view.findViewById(R.id.alarm_start_time_interaction_view)
        mEndTime = view.findViewById(R.id.alarm_end_time_interaction_view)
        mMessageEditor = view.findViewById(R.id.alarm_custom_message_edit_text)
        mDeleteButton = view.findViewById(R.id.alarm_delete_button)

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
            mListener?.onAlarmEnabled(mAlarmID, isChecked)
        }

        mStartTime.setOnClickListener{
            TimePickerFragment().show(childFragmentManager, startTimePickerTag)
        }

        mEndTime.setOnClickListener{
            TimePickerFragment().show(childFragmentManager, endTimePickerTag)
        }

        // TODO might want to add onFocusChangeListener or something to make editing easier (not working with SwiftKey)
        mMessageEditor.setOnEditorActionListener { view, actionId, event ->
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

        mDeleteButton.setOnClickListener{
            mRealm.executeTransaction {
                it.where(AlarmPeriod::class.java).equalTo("id", mAlarmID).findAll().deleteAllFromRealm()
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mRealm.close()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is AlarmPeriodFragmentListener) {
            mListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement AlarmPeriodFragmentListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    //TODO test verifying, check if Realm transaction should be moved from here to Activity as well
    override fun onPickerTimeSet(hours: Int, minutes: Int, fragmentTag: String) {
        Log.d("AlarmPeriodFragment", "onPickerTimeSet")
        mRealm.executeTransaction {
            // get all enabled alarms, except the one where the time is being changed
            var enabledAlarms = it.where(AlarmPeriod::class.java).equalTo("enabled", true)
                .and().notEqualTo("id", mAlarmID).findAll()

            mAlarmPeriod?.let {

                // use fragment tag to determine which picker was just set
                // and check validity against other enabled alarms before applying the change
                if(fragmentTag == startTimePickerTag){
                    if(Utils.isNewAlarmValid(AlarmPeriod(hours*60+minutes, it.endMinutes), enabledAlarms)){
                        it.startMinutes = hours*60+minutes
                    }else {
                        // show error message
                        view?.let {
                            Snackbar.make(it, R.string.toast_alarm_time_invalid, Toast.LENGTH_LONG).show()
                        }
                    }
                    mStartTime.text = it.startTimeAsString()
                }
                else if(fragmentTag == endTimePickerTag) {
                    if(Utils.isNewAlarmValid(AlarmPeriod(it.startMinutes, hours*60+minutes), enabledAlarms)){
                        it.endMinutes = hours*60+minutes
                    }else{
                        // show error message
                        view?.let {
                            Snackbar.make(it, R.string.toast_alarm_time_invalid, Toast.LENGTH_LONG).show()
                        }
                    }
                    mEndTime.text = it.endTimeAsString()
                }
            }
        }
    }

    interface AlarmPeriodFragmentListener{
        fun onAlarmDeleted()
        fun onAlarmEnabled(alarmID: String, enabled: Boolean)
        fun onAlarmTimeChanged(alarmPeriod: AlarmPeriod, hours: Int, minutes: Int, timeView: TextView)
    }
}