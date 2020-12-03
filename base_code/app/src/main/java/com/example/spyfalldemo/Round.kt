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
    private lateinit var roles : HashMap<String, MutableList<String>>

    companion object {
        const val MIN_PLAYERS = 2

        //!!!!!!!!!!!make sure the role sizes are consistent! right now there's FIVE roles!!!!!!!!!!!!!!!//
        val locations = arrayOf("Beach", "School", "Airport", "Park", "Gym")
        val beachRoles = mutableListOf("Lifeguard", "Tourists", "Sea Monster", "Child", "Food Vendor", "Role6", "Role7", "Role8", "Role9")
        val schoolRoles = mutableListOf("Student", "Teacher", "Janitor", "Librarian", "Principal", "Role6", "Role7", "Role8", "Role9")
        val airportRoles = mutableListOf("Security", "Flight Attendant", "Pilot", "Crying Baby", "Lost Child", "Role6", "Role7", "Role8", "Role9")
        val parkRoles = mutableListOf("Dog", "Tree", "Bench", "Photographer", "Painter", "Role6", "Role7", "Role8", "Role9")
        val gymRoles = mutableListOf("Body Builder", "Trainer", "Weak Potato", "Yoga Instructor", "Boxer", "Role6", "Role7", "Role8", "Role9")
        val colors = mutableListOf("#ffffff","#000000","#ff0000","#0000ff","#00ff00","#000000","#f0f0f0","#0f0f0f","#121212","#343434")
    }

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

        roles = HashMap<String, MutableList<String>>()
        roles["Beach"] = beachRoles
        roles["School"] = schoolRoles
        roles["Airport"] = airportRoles
        roles["Park"] = parkRoles
        roles["Gym"] = gymRoles
        //val randRole = (roles.indices).random()
        //roles[location]!![randRole].toString()

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
            if(players.size >= MIN_PLAYERS){
                databaseRoom.child("inGame").setValue(true)

                // reset previous game variables
                databaseRoom.child("spyWins").setValue(false)
                databaseRoom.child("civilianWins").setValue(false)
                databaseRoom.child("messages").setValue(HashMap<String,ChatMessage>())

                // assign location
                val randLocation = locations.indices.random()
                val location = locations[randLocation]
                databaseRoom.child("location").setValue(location)

                // assign spy
                val randSpy = players.indices.random()
                val spyID = players[randSpy].id
                databaseRoom.child("spy").setValue(spyID)

                roles[location]!!.shuffle()
                colors.shuffle()
                // reset player vote and roles
                // real jank
                var offset = 0
                val playersMap = HashMap<String, Player>()
                for(i in players.indices) {
                    val player = players[i]
                    if (player.id != spyID) {
                        val role = roles[location]?.get(i - offset).toString()
                        playersMap[player.id] = Player(player.id, player.name, role, "", colors[i])
                    } else {
                        playersMap[player.id] = Player(player.id, player.name, "Spy", "", colors[i])
                        offset = 1
                    }
                }
                databaseRoom.child("players").setValue(playersMap)

            } else {
                Toast.makeText(applicationContext, "Waiting for for more players...", Toast.LENGTH_SHORT).show()
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