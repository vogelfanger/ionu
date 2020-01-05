package com.ionu

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import io.realm.Realm
import io.realm.RealmConfiguration

class Ionu : Application() {

    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
        var config : RealmConfiguration = RealmConfiguration.Builder().name("myrealm.realm").build()
        Realm.setDefaultConfiguration(config)

        // set up notification channels
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // main channel
            val name = getString(R.string.app_name)
            val descriptionText = getString(R.string.notification_channel_description_main)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(GlobalVariables.MAIN_CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }

    }
}