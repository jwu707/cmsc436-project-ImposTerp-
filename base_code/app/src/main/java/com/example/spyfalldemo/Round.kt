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
    private lateinit var btnLeave: Button
    private lateinit var players: MutableList<Player>
    private lateinit var playerID : String
    private lateinit var playerName : String
    private lateinit var roomID : String
    private lateinit var databaseRoom: DatabaseReference
    private lateinit var databaseRoomPlayers: DatabaseReference
    private lateinit var databaseRoomLeave: DatabaseReference
    private var host = ""

    private val locationsArray = arrayOf("Beach", "School", "Airport", "Park", "Gym")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting)

        listView = findViewById(R.id.ListView)
        btnStart = findViewById(R.id.begin)
        btnLeave = findViewById(R.id.leave)
        txtRoomName = findViewById(R.id.room_name)

        roomID = intent.getStringExtra("ROOM_ID").toString()
        playerID = intent.getStringExtra("PLAYER_ID").toString()
        playerName = intent.getStringExtra("PLAYER_NAME").toString()
        host = intent.getStringExtra("HOST").toString()

        players = ArrayList()
        databaseRoom = FirebaseDatabase.getInstance().getReference("rooms").child(roomID)
        databaseRoomPlayers = databaseRoom.child("players")

        btnLeave.setOnClickListener(View.OnClickListener {
            if (host == playerID){
                databaseRoom.child("finished").setValue(true)
            }else {
                databaseRoomLeave = databaseRoomPlayers.child(playerID)
                databaseRoomLeave.removeValue()
                val backLobby = Intent(applicationContext, Rooms::class.java)
                backLobby.putExtra("PLAYER_ID", playerID)
                backLobby.putExtra("PLAYER_NAME", playerName)
                startActivity(backLobby)
            }
        })

        btnStart.setOnClickListener(View.OnClickListener {
            if(players.size <= 1){
                Toast.makeText(applicationContext, "Waiting for for more players...", Toast.LENGTH_SHORT).show()
            } else {
                databaseRoom.child("inGame").setValue(true)

                // reset previous game variables
                databaseRoom.child("spyWins").setValue(false)
                databaseRoom.child("civilianWins").setValue(false)

                val playersMap = HashMap<String, Player>()
                for(i in players.indices) {
                    val player = players[i]
                    playersMap[player.id] = Player(player.id, player.name)
                }
                databaseRoom.child("players").setValue(playersMap)

                //check with Play.kt to see the # of location and roles,//
                // right now there are 5 locations and 5 roles per locations//
                val randLoc = (0 until locationsArray.size).random()
                val randSpy = (0 until players.size).random()
                val location = locationsArray[randLoc]
                val spyID = players[randSpy].id
                databaseRoom.child("location").setValue(location)
                databaseRoom.child("spy").setValue(spyID)
            }
        })
    }

    private fun generateGameInfo() {

    }

    private fun startGame() {
        val intent = Intent(applicationContext, Play::class.java)
        intent.putExtra("ROOM_ID", roomID)
        intent.putExtra("PLAYER_ID", playerID)
        intent.putExtra("HOST", host)
        intent.putExtra("PLAYER_NAME", playerName)
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()

        databaseRoom.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //make sure to update before staring//
                var gameStat = false
                var locStat = false
                var spyStat = false

                for (postSnapshot in dataSnapshot.children) {
                    try {
                        if (postSnapshot.key == "inGame") {
                            if (postSnapshot.value as Boolean) {
                                gameStat = true
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
                        if (postSnapshot.key == "finished"){
                            if (postSnapshot.value as Boolean){
                                databaseRoom.removeValue()
                                Toast.makeText(applicationContext, "Host has left the game", Toast.LENGTH_SHORT).show()
                                val backLobby = Intent(applicationContext, Rooms::class.java)
                                backLobby.putExtra("PLAYER_ID", playerID)
                                backLobby.putExtra("PLAYER_NAME", playerName)
                                startActivity(backLobby)
                            }
                        }

                        /*
                        if (postSnapshot.key == "location"){
                            if (postSnapshot.value != ""){
                                loc = postSnapshot.value.toString()
                                locStat = true
                            }
                        }
                        if (postSnapshot.key == "spy"){
                            if (postSnapshot.value != ""){
                                spy = postSnapshot.value.toString()
                                spyStat = true
                            }
                        }
                        */
                    } catch (e: Exception) {

                    } finally {

                    }
                }
                if (gameStat/* && locStat && spySta*/){
                    startGame()
                }
            }
            override fun onCancelled(error: DatabaseError) {

            }
        })

        databaseRoomPlayers.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (databaseRoom == null){
                    val backLobby = Intent(applicationContext, Rooms::class.java)
                    startActivity(backLobby)
                } else {
                    players.clear()

                    var host: String = ""
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
            }
            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
}