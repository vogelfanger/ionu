package com.ionu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver for receiving screen-related intents.
 */
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
            Intent.ACTION_USER_PRESENT -> {
                Log.d("ScreenReceiver", "user present")
                context?.let {
                    it.startService(Intent(context, AlarmService::class.java)
                        .setAction(GlobalVariables.ACTION_USER_PRESENT))
                }
            }
        }
    }
}