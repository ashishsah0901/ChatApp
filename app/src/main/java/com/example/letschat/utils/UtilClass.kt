package com.example.letschat.utils

import android.text.format.DateFormat
import java.util.*

class UtilClass {
    companion object{
        fun getFormattedDate(timeInMillis: Long): String {
            val smsTime = Calendar.getInstance()
            smsTime.timeInMillis = timeInMillis
            val now = Calendar.getInstance()
            val timeFormattedString = "h:mm aa"
            val dateTimeFormattedString = "EEEE, MMMM d, h:mm aa"
            return if(now.get(Calendar.DATE)==smsTime.get(Calendar.DATE)){
                "Today ${DateFormat.format(timeFormattedString,smsTime)}"
            }else if(now.get(Calendar.DATE)-smsTime.get(Calendar.DATE)==1){
                "Yesterday ${DateFormat.format(timeFormattedString,smsTime)}"
            }else if(now.get(Calendar.YEAR)==smsTime.get(Calendar.YEAR)){
                DateFormat.format(dateTimeFormattedString,smsTime).toString()
            }else{
                DateFormat.format("MMMM dd yyyy, h:mm aa",smsTime).toString()
            }
        }
    }
}