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

// This activity allows the user to create their indiviudalized rooms for playing imposterp or join available rooms
// the host can create a room with edits to max players, time, and include a password
// the users looking to join a room can see the status of the games (player#, time, passowrd: required/notrequired, and current game status: ingame/notingame)

class Rooms : Activity() {

    //minimum players and time for a room
    companion object {
        const val MIN_PLAYERS = 3
        const val MIN_TIME = 1
    }
    //listView for the rooms available
    private lateinit var listView: ListView
    //create a room button
    private lateinit var btnCreate: Button

    //player ID, name, and list of rooms available
    private lateinit var playerID : String
    private lateinit var playerName : String
    private lateinit var rooms: MutableList<Room>

    //firebase references
    private lateinit var databaseRooms : DatabaseReference
    private lateinit var onChangeListenerRooms : ValueEventListener


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)

        //initalizing variables
        listView = findViewById(R.id.ListView)
        btnCreate = findViewById(R.id.create)

        playerID = intent.getStringExtra("PLAYER_ID").toString()
        playerName = intent.getStringExtra("PLAYER_NAME").toString()

        databaseRooms = FirebaseDatabase.getInstance().getReference("rooms")

        rooms = ArrayList()

        //onClickListener for creating a room
        btnCreate.setOnClickListener(View.OnClickListener {
            //a pop-up will be inflated giving the user ability to edit their hosted room
            val dialogBuilder = AlertDialog.Builder(this)
            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.create_room_dialog, null)
            dialogBuilder.setView(dialogView)

            val txtRoomName = dialogView.findViewById(R.id.room_name) as EditText
            val txtPassword = dialogView.findViewById(R.id.password) as TextView

            //# of players and the corresponding seekbar
            val lblPlayerCount = dialogView.findViewById(R.id.player_count_num) as TextView
            val barPlayerCount = dialogView.findViewById(R.id.player_count_seekbar) as SeekBar

            //# of minutes of discussion and corresponding the seekbar
            val time = dialogView.findViewById(R.id.time) as TextView
            val barTime = dialogView.findViewById(R.id.time_seekbar) as SeekBar

            val btnConfirm = dialogView.findViewById(R.id.confirm) as Button

            //Room name
            txtRoomName.setText(playerName + "'s game")

            //# od player seekbar controls
            barPlayerCount.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                    lblPlayerCount.text = (progress + MIN_PLAYERS).toString()
                }
                override fun onStartTrackingTouch(seek: SeekBar) {}
                override fun onStopTrackingTouch(seek: SeekBar) {}
            })

            //# of minutes seekbar controls
            barTime.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                    time.text = (progress + MIN_TIME).toString()
                }
                override fun onStartTrackingTouch(seek: SeekBar) {}
                override fun onStopTrackingTouch(seek: SeekBar) {}
            })
            val b = dialogBuilder.create()

            //confirm room edits
            btnConfirm.setOnClickListener{
                val players = barPlayerCount.progress + MIN_PLAYERS
                val time = barTime.progress + MIN_TIME
                //create a room with the edits
                addRoom(txtRoomName.text.toString(), txtPassword.text.toString(), players, time)
                b.dismiss()
            }
            b.show()
        })

        //Joining the rooms in listView
        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val room = rooms[position]
            if (!room.inGame) { //check game status
                if (room.players.size < room.maxPlayers) { //check max player
                    if (room.password == "") { //check password
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

    //make sure the password is correct when inputed
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

    //intent that takes the users into Round.kt
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

    //adds a room to the firebase database
    private fun addRoom(roomName : String, password : String, playerCount : Int, time : Int) {
        val id = databaseRooms.push().key
        val room = Room(id!!, roomName, password, playerCount, time, playerID)
        room.players[playerID] = Player(playerID, playerName)
        databaseRooms.child(id).setValue(room)
        joinRoom(room)
    }

    override fun onStart() {
        super.onStart()

        //chnage the listview everytime a room is added/deleted
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
