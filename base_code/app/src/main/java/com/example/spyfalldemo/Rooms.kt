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
import android.graphics.Color
import android.net.Uri
import android.widget.*
import androidx.appcompat.app.AlertDialog
import java.lang.Exception
import java.util.ArrayList

class Rooms : Activity() {

    private lateinit var listView: ListView
    private lateinit var btnCreate: Button
    private lateinit var rooms: MutableList<Room>
    private lateinit var databaseRooms : DatabaseReference
    private lateinit var playerID : String
    private lateinit var playerName : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)

        listView = findViewById(R.id.ListView)
        btnCreate = findViewById(R.id.create)

        playerID = intent.getStringExtra("PLAYER_ID").toString()
        playerName = intent.getStringExtra("PLAYER_NAME").toString()

        databaseRooms = FirebaseDatabase.getInstance().getReference("rooms")

        rooms = ArrayList()

        btnCreate.setOnClickListener(View.OnClickListener {
            val dialogBuilder = AlertDialog.Builder(this)
            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.create_room_dialog, null)
            dialogBuilder.setView(dialogView)

            val txtRoomName = dialogView.findViewById<View>(R.id.room_name) as EditText
            val lblPlayerCount = dialogView.findViewById<View>(R.id.player_count) as TextView
            val barPlayerCount = dialogView.findViewById<View>(R.id.player_count_seekbar) as SeekBar
            val btnConfirm = dialogView.findViewById<View>(R.id.confirm) as Button

            txtRoomName.setText(playerName + "'s game")

            barPlayerCount.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                    lblPlayerCount.text = "Max Players - " + progress
                }
                override fun onStartTrackingTouch(seek: SeekBar) {}
                override fun onStopTrackingTouch(seek: SeekBar) {}
            })

            // TODO : set min progress to 3
            btnConfirm.setOnClickListener{
                if (barPlayerCount.progress < 3){
                    Toast.makeText(applicationContext, "Cannot play with less than 3 players", Toast.LENGTH_SHORT).show()
                } else{
                    addRoom(txtRoomName.text.toString(), barPlayerCount.progress)
                }

            }

            dialogBuilder.setTitle("Create Room")
            val b = dialogBuilder.create()
            b.show()
        })

        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val room = rooms[position]

            if (!room.inGame) {
                if (room.players.size < room.maxPlayers) {
                    // add player to room
                    room.players[playerID] = Player(playerID, playerName)
                    databaseRooms.child(room.id).child("players").setValue(room.players)

                    joinRoom(room)
                } else {
                    Toast.makeText(this, "This lobby is full!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "This lobby is already in game!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun joinRoom(room : Room) {
        // start intent to create room
        val intent = Intent(applicationContext, Round::class.java)
        intent.putExtra("ROOM_ID", room.id)
        intent.putExtra("PLAYER_ID", playerID)
        startActivity(intent)
        finish()
    }

    private fun addRoom(roomName : String, playerCount : Int) {
        val id = databaseRooms.push().key
        val room = Room(id!!, roomName, playerCount, playerID)
        room.players[playerID] = Player(playerID, playerName)
        databaseRooms.child(id).setValue(room)

        joinRoom(room)
    }

    override fun onStart() {
        super.onStart()

        databaseRooms.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                rooms.clear()

                var room: Room? = null
                for (postSnapshot in dataSnapshot.children) {
                    try {
                        room = postSnapshot.getValue(Room::class.java)
                    } catch (e: Exception) {

                    } finally {
                        rooms.add(room!!)
                    }
                }

                var adapter = RoomListAdapter(this@Rooms, rooms)
                listView.adapter = adapter
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }
}
