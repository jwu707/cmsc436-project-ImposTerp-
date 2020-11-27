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

class Rooms : Activity() {
    private lateinit var listView: ListView
    private lateinit var create: Button
    private lateinit var rooms: ArrayList<String>
    private var playerName = ""
    private var roomName = ""
    private lateinit var dataBase: FirebaseDatabase
    private lateinit var roomRef: DatabaseReference
    private lateinit var roomsRef: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)

        listView = findViewById(R.id.ListView)
        create = findViewById(R.id.create)

        dataBase = Firebase.database
        val preference = getSharedPreferences("PREFS", 0)
        playerName = preference.getString("playerName", "").toString()
        roomName = playerName

        rooms = ArrayList<String>()

        create.setOnClickListener(View.OnClickListener {
            create.setText("CREATING...")
            create.isEnabled = false
            roomName = playerName
            roomRef = dataBase.getReference("rooms/$roomName/Host: $playerName")
            addRoomEvent()
            roomRef.setValue(playerName)
        })
        listView.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                roomName = rooms.get(position)
                roomRef = dataBase.getReference("rooms/$roomName/Player: $playerName")
                addRoomEvent()
                roomRef.setValue(playerName)
            }
        addRoomsEvent()
    }

    private fun addRoomEvent() {
        roomRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                create.setText("CREATE ROOM")
                create.isEnabled = true
                val round = Intent(applicationContext, Round::class.java)
                round.putExtra("roomName", roomName)
                round.putExtra("playerName", playerName)
                startActivity(round)
            }

            override fun onCancelled(error: DatabaseError) {
                create.setText("CREATE ROOM")
                create.isEnabled = true
            }
        })
    }


    private fun addRoomsEvent() {
        roomsRef = dataBase.getReference("rooms")
        roomsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                rooms.clear()
                val roomIter = dataSnapshot.children
                for (snap in roomIter){
                    rooms.add(snap.key.toString())
                    var adaptor = ArrayAdapter<String>(applicationContext, android.R.layout.simple_list_item_1, rooms)
                    listView.adapter = adaptor
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
}
