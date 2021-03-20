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
    var message : String

    // start and end time as elapsed minutes from start of day (e.g. 06:30 is 390 minutes)
    var startMinutes : Int
    var endMinutes : Int

    init {
        // set default values
        id = UUID.randomUUID().toString()
        startMinutes = 0
        endMinutes = 0
        message = "";
    }

    constructor(startMinutes: Int, endMinutes: Int) : this(){
        this.startMinutes = validateTime(startMinutes)
        this.endMinutes = validateTime(endMinutes)
    }

    /**
     * Returns lenght of AlarmPeriod as minutes.
     * @return alarm period's lenght as minutes
     */
    fun lengthInMinutes() : Int{
        if(startMinutes > endMinutes){
            return 1440-startMinutes+endMinutes
        }else{
            return endMinutes - startMinutes
        }
    }

    /**
     * Returns string representation of AlarmPeriod's start and end times.
     * @return start and end times of AlarmPeriod
     */
    fun clockTimesAsString() : String{
        return startTimeAsString() + " - " +  endTimeAsString()
    }

    /**
     * Returns string representation of AlarmPeriod's start time.
     * @return start time of AlarmPeriod
     */
    fun startTimeAsString() : String{
        val startHour : Int = startMinutes/60
        val startMinute : Int = startMinutes-(startHour*60)
        return ("" + timeToString(startHour) + ":" + timeToString(startMinute))
    }

    /**
     * Returns string representation of AlarmPeriod's end time.
     * @return end time of AlarmPeriod
     */
    fun endTimeAsString() : String{
        val endHour : Int = endMinutes/60
        val endMinute : Int = endMinutes-(endHour*60)
        return ("" + timeToString(endHour) + ":" + timeToString(endMinute))
    }

    private fun timeToString(time: Int) : String{
        var string = ""
        if(time < 10){
            string += "0"
        }
        return string + time
    }

    private fun validateTime(timeAsMinutes: Int) : Int{
        if(timeAsMinutes > 1439 || timeAsMinutes < 0){
            return 0
        }else return timeAsMinutes
    }
}