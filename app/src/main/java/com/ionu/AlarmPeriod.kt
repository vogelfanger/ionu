package com.ionu

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import java.util.*

open class AlarmPeriod() : RealmObject() {

    @PrimaryKey
    @Required
    var id : String
    var enabled : Boolean = false
    var startHours : Int
    var startMinutes : Int
    var endHours : Int
    var endMinutes : Int
    var message : String

    // set default values
    init {
        id = UUID.randomUUID().toString()
        startHours = 0
        startMinutes = 0
        endHours = 0
        endMinutes = 0
        message = "";
    }

    constructor(startHours: Int, startMinutes: Int, endHours: Int, endMinutes: Int) : this(){
        this.startHours = validateHours(startHours)
        this.startMinutes = validateMinutes(startMinutes)
        this.endHours = validateHours(endHours)
        this.endMinutes = validateMinutes(endMinutes)
    }

    fun clockTimesAsString() : String{
        return ("" + timeToString(startHours) + ":" + timeToString(startMinutes) +
                " - " + timeToString(endHours) + ":" + timeToString(endMinutes))
    }

    fun startTimeAsString() : String{
        return ("" + timeToString(startHours) + ":" + timeToString(startMinutes))
    }

    fun endTimeAsString() : String{
        return ("" + timeToString(endHours) + ":" + timeToString(endMinutes))
    }

    private fun timeToString(time: Int) : String{
        var string = ""
        if(time < 10){
            string += "0"
        }
        return string + time
    }

    private fun validateHours(hours: Int) : Int{
        if(hours > 23 || hours < 0){
            return 0
        }else return hours
    }

    private fun validateMinutes(minutes: Int) : Int{
        if(minutes > 59 || minutes < 0){
            return 0
        }else return minutes
    }
}