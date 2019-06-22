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
            //TODO inform service that user opened screen during alarm
            Intent.ACTION_USER_PRESENT -> Log.d("ScreenReceiver", "user present")
            //TODO inform service that screen went off during alarm
            Intent.ACTION_SCREEN_OFF -> Log.d("ScreenReceiver", "screen off")
        }
    }


}