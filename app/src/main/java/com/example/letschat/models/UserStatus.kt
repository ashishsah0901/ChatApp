package com.example.letschat.models

data class UserStatus(
        val name:String="",
        val profileImage:String="",
        val lastUpdated: Long=0,
        var statuses:ArrayList<Status>?=null
)