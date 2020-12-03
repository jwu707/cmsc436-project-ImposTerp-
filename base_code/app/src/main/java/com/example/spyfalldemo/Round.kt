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
    private lateinit var grdPlayers: GridLayout
    private lateinit var lstChatLog: ListView
    private lateinit var btnSendMessage : Button
    private lateinit var edtEditMessage : EditText
    private lateinit var txtRoomName : TextView
    private lateinit var btnStart: Button
    private lateinit var btnLeave: Button
    private lateinit var players: MutableList<Player>
    private lateinit var playerID : String
    private lateinit var playerName : String
    private lateinit var roomID : String
    private lateinit var databaseRoom: DatabaseReference
    private lateinit var databaseRoomPlayers: DatabaseReference
    private lateinit var databaseRoomChatLog: DatabaseReference
    private lateinit var onChangeListenerRoom : ValueEventListener
    private lateinit var onChangeListenerPlayers : ValueEventListener
    private lateinit var onChangeListenerChatLog : ValueEventListener
    private lateinit var hostID : String

    private val locationsArray = arrayOf("Beach", "School", "Airport", "Park", "Gym")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting)

        grdPlayers = findViewById(R.id.players_grid)
        lstChatLog = findViewById(R.id.chat_log)
        edtEditMessage = findViewById(R.id.edit_message)
        btnSendMessage = findViewById(R.id.send_message)
        btnStart = findViewById(R.id.begin)
        btnLeave = findViewById(R.id.leave)
        txtRoomName = findViewById(R.id.room_name)

        roomID = intent.getStringExtra("ROOM_ID").toString()
        playerID = intent.getStringExtra("PLAYER_ID").toString()
        playerName = intent.getStringExtra("PLAYER_NAME").toString()

        players = ArrayList()
        databaseRoom = FirebaseDatabase.getInstance().getReference("rooms").child(roomID)
        databaseRoomPlayers = databaseRoom.child("players")
        databaseRoomChatLog = databaseRoom.child("messages")

        btnLeave.setOnClickListener{
            if (hostID == playerID){
                databaseRoom.child("finished").setValue(true)
            }else {
                // remove the player from the list of players
                databaseRoomPlayers.child(playerID).removeValue()
                leaveRoom()
                sendMessage("", playerName + " has left the game!")
            }
        }

        btnStart.setOnClickListener{
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
        }

        btnSendMessage.setOnClickListener{
            val msg = edtEditMessage.text.toString()
            sendMessage(playerName, msg)
            edtEditMessage.setText("")
        }
    }

    private fun sendMessage(sender : String, content : String) {
        // make sure message is not blank
        if (content != "") {
            val msg = ChatMessage(sender, content)
            databaseRoomChatLog.push().setValue(msg)
        }
    }

    private fun generateGameInfo() {

    }

    private fun startGame() {
        val intent = Intent(applicationContext, Play::class.java)
        intent.putExtra("ROOM_ID", roomID)
        intent.putExtra("PLAYER_ID", playerID)
        intent.putExtra("PLAYER_NAME", playerName)
        startActivity(intent)
        finish()
    }

    private fun leaveRoom() {
        val backLobby = Intent(applicationContext, Rooms::class.java)
        backLobby.putExtra("PLAYER_ID", playerID)
        backLobby.putExtra("PLAYER_NAME", playerName)
        startActivity(backLobby)
    }

    override fun onStart() {
        super.onStart()

        onChangeListenerRoom = databaseRoom.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //make sure to update before staring//
                var gameStat = false

                for (postSnapshot in dataSnapshot.children) {
                    if (postSnapshot.key == "inGame") {
                        if (postSnapshot.value as Boolean) {
                            gameStat = true
                        }
                    }
                    if (postSnapshot.key == "host") {
                        hostID = postSnapshot.value.toString()
                        if (postSnapshot.value != playerID) {
                            btnStart.visibility = View.GONE
                            var params = edtEditMessage.layoutParams as RelativeLayout.LayoutParams
                            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                            edtEditMessage.layoutParams = params
                        }
                    }
                    if (postSnapshot.key == "name") {
                        txtRoomName.text = postSnapshot.value.toString()
                    }

                    if (postSnapshot.key == "finished"){
                        if (postSnapshot.value as Boolean){
                            databaseRoom.removeValue()
                            Toast.makeText(applicationContext, "Host has left the game", Toast.LENGTH_SHORT).show()
                            leaveRoom()
                        }
                    }
                }
                if (gameStat){
                    startGame()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        onChangeListenerChatLog = databaseRoomChatLog.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var messages: MutableList<ChatMessage> = ArrayList()
                var message: ChatMessage? = null
                for (postSnapshot in dataSnapshot.children) {
                    message = postSnapshot.getValue(ChatMessage::class.java)
                    messages.add(message!!)
                }
                var adapter = ChatLogAdapter(this@Round, messages)
                lstChatLog.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        onChangeListenerPlayers = databaseRoomPlayers.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (databaseRoom == null){
                    leaveRoom()
                } else {
                    players.clear()
                    var player: Player? = null
                    for (postSnapshot in dataSnapshot.children) {
                        player = postSnapshot.getValue(Player::class.java)
                        players.add(player!!)
                    }

                    populatePlayersGrid()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        sendMessage("", playerName + " has joined the game!")
    }

    override fun onPause() {
        super.onPause()
        databaseRoom.removeEventListener(onChangeListenerRoom)
        databaseRoomPlayers.removeEventListener(onChangeListenerPlayers)
        databaseRoomChatLog.removeEventListener(onChangeListenerChatLog)
    }

    private fun populatePlayersGrid() {
        // reset layout
        grdPlayers.removeAllViews()

        for(i in players.indices) {
            val player = players[i]
            val plate = layoutInflater.inflate(R.layout.plate, null)
            var textView = plate.findViewById<TextView>(R.id.plate_text)
            // funny business
            if (player.id == playerID) {
                // hostID is sometimes null because onDataChange is asynchronous
                //if (player.id == hostID) {
                    //textView.text = "[Host] " + player.name + " (You!)"
                //} else {
                    textView.text = player.name + " (You!)"
                //}
            } else {
                textView.text = player.name
            }

            grdPlayers.addView(plate)

        }
    }
}