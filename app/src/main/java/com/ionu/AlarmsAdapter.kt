package com.ionu

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView

class AlarmsAdapter(private val myDataset: List<String>):
    RecyclerView.Adapter<AlarmsAdapter.AlarmViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val alarmView = LayoutInflater.from(parent.context)
            .inflate(R.layout.alarm_list_item, parent, false) as View
        return AlarmViewHolder(alarmView)
    }

    override fun getItemCount(): Int = myDataset.size

    // populate view with data
    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        // TODO set alarm text and switch values based on the dataset
        holder.alarmText.text = myDataset[position]
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
}