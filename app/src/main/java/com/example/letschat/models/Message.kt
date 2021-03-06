package com.example.letschat.models

data class Message(
    var message: String?=null,
    val senderID: String="",
    val timeStamp: Long=0,
    var imageUrl:String="",
    var messageID: String="",
    var feeling: Int = -1
)