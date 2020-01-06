package com.ionu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ScreenReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        if(intent == null) {
            return;
        }
        when(intent.action){
            Intent.ACTION_SCREEN_ON -> {
                Log.d("ScreenReceiver", "screen on")
                context?.let {
                    it.startService(Intent(context, AlarmService::class.java)
                        .setAction(GlobalVariables.ACTION_SCREEN_ON))
                }
            }
            //TODO inform service that user opened screen during alarm
            Intent.ACTION_USER_PRESENT -> {
                Log.d("ScreenReceiver", "user present")
                context?.let {
                    it.startService(Intent(context, AlarmService::class.java)
                        .setAction(GlobalVariables.ACTION_USER_PRESENT))
                }
            }
            //TODO inform service that screen went off during alarm
            Intent.ACTION_SCREEN_OFF -> {
                Log.d("ScreenReceiver", "screen off")
                context?.let {
                    it.startService(Intent(context, AlarmService::class.java)
                        .setAction(GlobalVariables.ACTION_SCREEN_OFF))
                }
            }
        }
    }


}