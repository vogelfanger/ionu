package com.ionu

class AlarmPeriod(startHours: Int, startMinutes: Int,
                  endHours: Int, endMinutes: Int) {

    var startTime: ClockTime
    var endTime: ClockTime

    init{
        startTime = ClockTime(startHours, startMinutes)
        endTime = ClockTime(endHours, endMinutes)
    }

    // Representation of clock time as number of hours and minutes (0-23h, 0-59min)
    class ClockTime(var hours: Int, var minutes: Int){

        // force input to valid clock time
        init {
            if(hours > 23 || hours < 0) {
                hours = 0
            }
            if(minutes > 59 || minutes < 0) {
                minutes = 0
            }
        }
    }
}