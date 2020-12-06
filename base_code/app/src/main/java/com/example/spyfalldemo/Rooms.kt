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

    companion object {
        const val MIN_PLAYERS = 2
        const val MIN_TIME = 1
    }

    private lateinit var listView: ListView
    private lateinit var btnCreate: Button
    private lateinit var rooms: MutableList<Room>
    private lateinit var databaseRooms : DatabaseReference
    private lateinit var playerID : String
    private lateinit var playerName : String
    private lateinit var onChangeListenerRooms : ValueEventListener

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

            val txtRoomName = dialogView.findViewById(R.id.room_name) as EditText
            val txtPassword = dialogView.findViewById(R.id.password) as TextView
            val lblPlayerCount = dialogView.findViewById(R.id.player_count_num) as TextView
            val barPlayerCount = dialogView.findViewById(R.id.player_count_seekbar) as SeekBar
            val btnConfirm = dialogView.findViewById(R.id.confirm) as Button
            val time = dialogView.findViewById(R.id.time) as TextView
            val barTime = dialogView.findViewById(R.id.time_seekbar) as SeekBar

            txtRoomName.setText(playerName + "'s game")

            barPlayerCount.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                    lblPlayerCount.text = (progress + MIN_PLAYERS).toString()
                }
                override fun onStartTrackingTouch(seek: SeekBar) {}
                override fun onStopTrackingTouch(seek: SeekBar) {}
            })

            barTime.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                    time.text = (progress + MIN_TIME).toString()
                }
                override fun onStartTrackingTouch(seek: SeekBar) {}
                override fun onStopTrackingTouch(seek: SeekBar) {}
            })

            val b = dialogBuilder.create()
            btnConfirm.setOnClickListener{
                val players = barPlayerCount.progress + MIN_PLAYERS
                val time = barTime.progress + MIN_TIME
                addRoom(txtRoomName.text.toString(), txtPassword.text.toString(), players, time)
                b.dismiss()
            }
            b.show()
        })

        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val room = rooms[position]
            if (!room.inGame) {
                if (room.players.size < room.maxPlayers) {
                    if (room.password == "") {
                        joinRoom(room)
                    } else {
                        confirmPassword(room)
                    }

                } else {
                    Toast.makeText(this, "This lobby is full!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "This lobby is already in game!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmPassword(room : Room) {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.request_password, null)
        dialogBuilder.setView(dialogView)

        val edtPassword = dialogView.findViewById(R.id.password) as EditText
        val btnConfirm = dialogView.findViewById(R.id.confirm) as Button

        val b = dialogBuilder.create()
        btnConfirm.setOnClickListener{
            val password = edtPassword.text.toString()
            if (password == room.password) {
                joinRoom(room)
                b.dismiss()
            } else {
                Toast.makeText(
                    this@Rooms,
                    "Incorrect Password!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        b.show()
    }

    private fun joinRoom(room : Room) {
        room.players[playerID] = Player(playerID, playerName)
        databaseRooms.child(room.id).child("players").setValue(room.players)
        // start intent to create room
        val intent = Intent(applicationContext, Round::class.java)
        intent.putExtra("ROOM_ID", room.id)
        intent.putExtra("PLAYER_ID", playerID)
        intent.putExtra("PLAYER_NAME", playerName)
        startActivity(intent)
        finish()
    }

    private fun addRoom(roomName : String, password : String, playerCount : Int, time : Int) {
        val id = databaseRooms.push().key
        val room = Room(id!!, roomName, password, playerCount, time, playerID)
        room.players[playerID] = Player(playerID, playerName)
        databaseRooms.child(id).setValue(room)
        joinRoom(room)
    }

    override fun onStart() {
        super.onStart()

        onChangeListenerRooms = databaseRooms.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                rooms.clear()
                var room: Room? = null
                for (postSnapshot in dataSnapshot.children) {
                    room = postSnapshot.getValue(Room::class.java)
                    rooms.add(room!!)
                }
                var adapter = RoomListAdapter(this@Rooms, rooms)
                listView.adapter = adapter
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    @Override
    override fun onPause() {
        super.onPause()
        databaseRooms.removeEventListener(onChangeListenerRooms)
    }

    /*
    @Override
    override fun onDestroy() {
        super.onDestroy()
        FirebaseDatabase.getInstance().getReference("players").child(playerID).removeValue()
    }
    */

    @Override
    override fun onBackPressed()
    {
        val backMain = Intent(applicationContext, MainActivity::class.java)
        FirebaseDatabase.getInstance().getReference("players").child(playerID).removeValue()
        startActivity(backMain)
        finish()
    }
}
