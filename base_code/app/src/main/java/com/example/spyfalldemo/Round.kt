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
import java.lang.Exception
import java.util.ArrayList

class Round : Activity(){
    private lateinit var listView: ListView
    private lateinit var txtRoomName : TextView
    private lateinit var btnStart: Button
    private lateinit var players: MutableList<Player>
    private lateinit var playerID : String
    private lateinit var roomID : String
    private lateinit var databaseRoom: DatabaseReference
    private lateinit var databaseRoomPlayers: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting)

        listView = findViewById(R.id.ListView)
        btnStart = findViewById(R.id.begin)
        txtRoomName = findViewById(R.id.room_name)

        roomID = intent.getStringExtra("ROOM_ID").toString()
        playerID = intent.getStringExtra("PLAYER_ID").toString()

        players = ArrayList()

        databaseRoom = FirebaseDatabase.getInstance().getReference("rooms").child(roomID)
        databaseRoomPlayers = FirebaseDatabase.getInstance().getReference("rooms").child(roomID).child("players")

        btnStart.setOnClickListener(View.OnClickListener {
            if(players.size <= 2){
                Toast.makeText(applicationContext, "Waiting for for more players...", Toast.LENGTH_SHORT).show()
            } else {
                databaseRoom.child("inGame").setValue(true)
            }
        })
        starting()
    }

    private fun starting() {
        /*
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
                btnStart.setText("START ROUND")
                btnStart.isEnabled = true
            }
        })
        */
    }

    private fun startGame() {
        val intent = Intent(applicationContext, Play::class.java)
        intent.putExtra("ROOM_ID", roomID)
        intent.putExtra("PLAYER_ID", playerID)
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()

        databaseRoom.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (postSnapshot in dataSnapshot.children) {
                    try {
                        if (postSnapshot.key == "inGame") {
                            if (postSnapshot.value as Boolean) {
                                startGame()
                            }
                        }
                        if (postSnapshot.key == "host") {
                            if (postSnapshot.value != playerID) {
                                btnStart.visibility = View.GONE
                            }
                        }
                        if (postSnapshot.key == "name") {
                            txtRoomName.text = postSnapshot.value.toString()
                        }
                    } catch (e: Exception) {

                    } finally {

                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {

            }
        })

        databaseRoomPlayers.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                players.clear()

                var host : String = ""
                var player: Player? = null
                for (postSnapshot in dataSnapshot.children) {
                    // set host id in ondatachanged above?
                    if (postSnapshot.key == "host") {
                        host = postSnapshot.value.toString()
                    }

                    try {
                        player = postSnapshot.getValue(Player::class.java)
                    } catch (e: Exception) {

                    } finally {
                        players.add(player!!)
                    }
                }

                var adapter = LobbyPlayerListAdapter(this@Round, players, host)
                listView.adapter = adapter
            }
            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
}