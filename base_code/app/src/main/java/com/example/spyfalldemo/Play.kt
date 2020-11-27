package com.example.spyfalldemo

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.*
import java.util.ArrayList
import kotlin.random.Random


class Play : Activity(){

//    private lateinit var dataBase: FirebaseDatabase
//    private lateinit var playerRef: DatabaseReference
//    private lateinit var locRef: DatabaseReference
//    private lateinit var spyRef: DatabaseReference
//
//    val location = arrayOf("Beach", "School", "Airport", "Park", "Gym")
//    val max = location.size
//    var roomName = ""
//    var playerName = ""
//    var size = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)
//        val randLoc = (0 until max).random()
//        dataBase = Firebase.database
//        roomName = intent.getStringExtra("roomName").toString()
//        playerName = intent.getStringExtra("playerName").toString()
//        size = intent.getStringExtra("size")!!.toInt()
//        val randSpy = (0 until size).random()
//        locRef = dataBase.getReference("rooms/$roomName/Host: $roomName/location")
//        spyRef = dataBase.getReference("rooms/$roomName/Host: $roomName/spy")


    }
}