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

        holder.alarmText.text = mDataset.get(position).clockTimesAsString()
        holder.alarmText.setOnClickListener{
            mListener.onAlarmTextClicked(mDataset.get(position))
        }
        holder.alarmSwitch.isChecked = mDataset.get(position).enabled
        holder.alarmSwitch.setOnCheckedChangeListener{ view, isChecked ->
            mListener.onAlarmSwitchToggled(mDataset.get(position))
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
        fun onAlarmSwitchToggled(item : AlarmPeriod)
        fun onAlarmTextClicked(item : AlarmPeriod)
    }

}