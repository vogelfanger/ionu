package com.ionu

import android.app.Application
import io.realm.Realm
import io.realm.RealmConfiguration

class Ionu : Application() {

    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
        var config : RealmConfiguration = RealmConfiguration.Builder().name("myrealm.realm").build()
        Realm.setDefaultConfiguration(config)
    }
}