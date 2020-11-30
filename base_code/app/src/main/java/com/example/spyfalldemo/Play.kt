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
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_play.*
import java.lang.Exception
import java.util.ArrayList
import kotlin.random.Random


class Play : Activity(){

    private lateinit var playerRef: DatabaseReference
    private lateinit var roomRef: DatabaseReference
    private lateinit var roleView : TextView
    private lateinit var locView : TextView

    val location = arrayOf("Beach", "School", "Airport", "Park", "Gym")

    //!!!!!!!!!!!make sure the role sizes are consistent! right now theres FIVE roles!!!!!!!!!!!!!!!//
    val roleSize = 5

    val beachRoles = arrayOf("Lifeguard", "Tourists", "Sea Monster", "Child", "Food Vendor")
    val schoolRoles = arrayOf("Student", "Teacher", "Janitor", "Librarian", "Principal")
    val airportRoles = arrayOf("Security", "Flight Attendant", "Pilot", "Crying Baby", "Lost Child")
    val parkRoles = arrayOf("Dog", "Tree", "Bench", "Photographer", "Painter")
    val gymRoles = arrayOf("Body Builder", "Trainer", "Weak Potato", "Yoga Instructor", "Boxer")



    private var roles = HashMap<String, Array<String>> ()
    private var roomID = ""
    private var playerID = ""
    private var randRole = 0
    private var loc = ""
    private var spy = ""



    private lateinit var players : ArrayList<Player>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        roleView = findViewById(R.id.textView3)
        locView = findViewById(R.id.textView)

        roles["Beach"] = beachRoles
        roles["School"] = schoolRoles
        roles["Airport"] = airportRoles
        roles["Park"] = parkRoles
        roles["Gym"] = gymRoles

        roomID = intent.getStringExtra("ROOM_ID").toString()
        playerID = intent.getStringExtra("PLAYER_ID").toString()
        loc = intent.getStringExtra("LOCATION").toString()
        spy = intent.getStringExtra("SPY").toString()


        randRole = (0 until roleSize).random()
        if (playerID == spy){
            roleView.text = "SPY"
            locView.text = "Guess the Location to WIN!"
        }else{
            roleView.text = roles.get(loc)!![randRole].toString()
            locView.text = "Location: " + loc
        }

    }



    private fun castVote(playerID : String) {

    }

    private fun guess() {

    }
}