package com.ionu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.realm.Realm

/**
 * BroadcastReceiver for receiving boot-related intents.
 * TODO this hasn't been tested, apparently boot intents cannot be used on Huawei devices
 */
class BootReceiver : BroadcastReceiver() {

    private val TAG = "BootReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "boot intent received")
        if(intent == null) {
            return;
        }

        when(intent.action){
            // TODO seems that Huawei devices won't trigger this at all, need to find an alternative
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "ACTION_BOOT_COMPLETED")
                context?.let {
                    var realm = Realm.getDefaultInstance()
                    val nextAlarmMillis = Utils.getNextAlarmMillis(realm, System.currentTimeMillis())
                    if(nextAlarmMillis > -1){
                        Log.d(TAG, "scheduling service to start at " + nextAlarmMillis)
                        Utils.scheduleAlarmService(nextAlarmMillis, it)
                    } else {
                        Log.d(TAG, "no active alarm periods found")
                    }
                    realm.close()
                }
            }
        }
    }
}