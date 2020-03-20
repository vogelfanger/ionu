package com.ionu

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import java.util.*

open class HistoryPeriod() : RealmObject() {

    @PrimaryKey
    @Required
    var id : String
    var startMillis : Long
    var endMillis : Long
    var successful : Boolean

    init {
        id = UUID.randomUUID().toString()
        startMillis = 0L
        endMillis = 0L
        successful = true
    }

    constructor(startMillis : Long, endMillis : Long, successful : Boolean) : this() {
        this.startMillis = startMillis
        this.endMillis = endMillis
        this.successful = successful
    }

    public fun getLenght() : Long {
        return endMillis - startMillis
    }
}