package com.ionu

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import io.realm.OrderedRealmCollection
import io.realm.RealmRecyclerViewAdapter

class AlarmsAdapter(private val mListener: OnAlarmItemClickListener,
                    private val mDataset : OrderedRealmCollection<AlarmPeriod>):
                        RealmRecyclerViewAdapter<AlarmPeriod, AlarmsAdapter.AlarmViewHolder>(
                            mDataset, true) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val alarmView = LayoutInflater.from(parent.context)
            .inflate(R.layout.alarm_list_item, parent, false) as View
        return AlarmViewHolder(alarmView)
    }

    override fun getItemCount(): Int = mDataset.size

    // populate view with data
    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        // set listeners to null to avoid exceptions when data is changed while recyclerview computes things
        holder.alarmText.setOnClickListener(null)
        holder.alarmSwitch.setOnCheckedChangeListener(null)

        holder.alarmText.text = mDataset[position].clockTimesAsString()
        holder.alarmText.setOnClickListener{
            mListener.onAlarmTextClicked(mDataset[position])
        }
        holder.alarmSwitch.isChecked = mDataset[position].enabled
        holder.alarmSwitch.setOnCheckedChangeListener{ view, isChecked ->
            mListener.onAlarmSwitchToggled(mDataset[position].id, isChecked)
        }
    }

    // construct a viewholder
    class AlarmViewHolder(alarmView: View): RecyclerView.ViewHolder(alarmView){

        var alarmSwitch: Switch
        var alarmText: TextView

        init {
            alarmSwitch = alarmView.findViewById(R.id.alarm_switch)
            alarmText = alarmView.findViewById(R.id.alarm_time_view)
        }

    }

    interface OnAlarmItemClickListener {
        fun onAlarmSwitchToggled(alarmID : String, isChecked: Boolean)
        fun onAlarmTextClicked(item : AlarmPeriod)
    }

}