package com.ionu

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.widget.TimePicker
import java.util.*

class TimePickerFragment : DialogFragment(), TimePickerDialog.OnTimeSetListener {

    lateinit var mListener : TimePickerListener
    lateinit var mTag : String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        // Create TimePickerDialog using current time as default value
        val calendar = Calendar.getInstance()
        val hours = calendar.get(Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(Calendar.MINUTE)
        return TimePickerDialog(activity, this, hours, minutes, true)
    }

    override fun onTimeSet(p0: TimePicker?, hours: Int, minutes: Int) {
        mListener.onPickerTimeSet(hours, minutes, mTag)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment is TimePickerFragment.TimePickerListener) {
            mListener = parentFragment as TimePickerListener
        } else {
            throw RuntimeException(context.toString() + " must implement TimePickerListener")
        }
    }

    override fun show(manager: FragmentManager?, tag: String?) {
        super.show(manager, tag)
        if(tag != null) mTag = tag
        else mTag = ""
    }

    interface TimePickerListener{
        fun onPickerTimeSet(hours: Int, minutes: Int, fragmentTag: String)
    }
}