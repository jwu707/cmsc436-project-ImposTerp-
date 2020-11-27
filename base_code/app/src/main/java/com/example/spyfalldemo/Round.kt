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

class Round : Activity(){
    private lateinit var listView: ListView
    private lateinit var round: Button
    private lateinit var rooms: ArrayList<String>
    private var playerName = ""
    private var roomName = ""
    private lateinit var dataBase: FirebaseDatabase
    private lateinit var startRef: DatabaseReference
    private lateinit var playersRef: DatabaseReference
    private lateinit var roomRef: DatabaseReference
    var clicked = false
    var size = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting)

        listView = findViewById(R.id.ListView)
        round = findViewById(R.id.begin)

        dataBase = Firebase.database
        roomName = intent.getStringExtra("roomName").toString()
        playerName = intent.getStringExtra("playerName").toString()
        rooms = ArrayList<String>()
        startRef = dataBase.getReference("rooms/$roomName/Host: $roomName/status")





        round.setOnClickListener(View.OnClickListener {
            if( size <= 2){
                Toast.makeText(applicationContext, "Waiting for for more players...", Toast.LENGTH_SHORT).show()
            } else if(playerName == roomName){
                clicked == true
                round.setText("STARTING...")
                round.isEnabled = false
                starting()
                startRef.setValue("started")

            } else{
                Toast.makeText(applicationContext, "Waiting for Host", Toast.LENGTH_SHORT).show()
            }
        })
        starting()
        addRoomsEvent()

    }

    private fun starting() {
        startRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.getValue(true) == "started"){
                    round.setText("START ROUND")
                    round.isEnabled = true
                    val play = Intent(applicationContext, Play::class.java)
                    play.putExtra("roomName", roomName)
                    play.putExtra("playerName", playerName)
                    play.putExtra("size", size)
                    startActivity(play)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                round.setText("START ROUND")
                round.isEnabled = true
            }
        })
    }

    private fun addRoomsEvent() {
        playersRef = dataBase.getReference("rooms/$roomName/")
        playersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                rooms.clear()
                val roomIter = dataSnapshot.children
                size = 0
                for (snap in roomIter){
                    rooms.add(snap.key.toString())
                    var adaptor = ArrayAdapter<String>(applicationContext, android.R.layout.simple_list_item_1, rooms)
                    listView.adapter = adaptor
                    size = size + 1
                }
            }
            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
}