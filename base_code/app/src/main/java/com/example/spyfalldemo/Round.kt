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
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import java.lang.Exception
import java.util.ArrayList

//The Round activity is a waiting room for the players to wait for other players to join
//users are free to leave the game at anytime, however if the host happens to leave every user will also be kicked out of the room back into Room.kt
//in addition, the players in the room will be able to chat with one another and will be updated when another player joins
//this activity also takes care of the distribution of roles, location, and colors through the host

class Round : Activity(){
    //player grid
    private lateinit var grdPlayers: GridLayout

    //variables for chat
    private lateinit var lstChatLog: ListView
    private lateinit var btnChatClose : Button
    private lateinit var chatLayout : RelativeLayout
    private lateinit var edtEditMessage : EditText

    //room name, start and leave game button
    private lateinit var txtRoomName : TextView
    private lateinit var btnStart: Button
    private lateinit var btnLeave: Button

    //player, room, role, and host variables
    private lateinit var players: MutableList<Player>
    private lateinit var playerID : String
    private lateinit var playerName : String
    private lateinit var roomID : String
    private lateinit var hostID : String
    private lateinit var roles : HashMap<String, MutableList<String>>

    //firbase referneces
    private lateinit var databaseRoom: DatabaseReference
    private lateinit var databaseRoomPlayers: DatabaseReference
    private lateinit var databaseRoomChatLog: DatabaseReference
    private lateinit var onChangeListenerRoom : ValueEventListener
    private lateinit var onChangeListenerPlayers : ValueEventListener
    private lateinit var onChangeListenerChatLog : ValueEventListener


    companion object {
        const val MIN_PLAYERS = Rooms.MIN_PLAYERS

        //List of all the locations
        val locations = arrayOf(
            "Capital One Field",
            "Memorial Chapel",
            "Marathon Deli",
            "Dining Hall",
            "The Varsity",
            "The Stamp",
            "McKeldin",
            "Clarice",
            "Eppley",
            "Iribe"
        )

        //listd of roles at each corresponding locations
        val stampRoles = mutableListOf("Janitor", "Receptionist", "Student", "Student", "Fast Food Worker", "Cashier", "Cashier")
        val iribeRoles = mutableListOf("Larry Herman", "Clyde Kruskal", "Adviser", "Teaching Assistant", "Dying CS Student", "Teaching Assistant", "Dying CS Student")
        val fieldRoles = mutableListOf("Football Player", "Crowd Member", "Cheer Leader", "Coach", "Food Vendor", "Football Player", "Crowd Member")
        val eppleyRoles = mutableListOf("Weightlifter", "Runner", "Personal Trainer", "Body Builder", "Yoga Instructor", "Karate Instructor", "Jogger")
        val libraryRoles = mutableListOf("Student", "Librarian", "Book", "StarBucks Employee", "Student", "Librarian", "Book")
        val clariceRoles = mutableListOf("Actor", "Pianist", "Orchestra", "Conductor", "Singer", "Violinist", "Dancer")
        val deliRoles = mutableListOf("Janitor", "Manager", "Cook", "Cashier", "Dish Washer", "Customer", "Restocker")
        val diningRoles = mutableListOf("Cook", "Server", "Janitor", "Dish Washer", "Hungry Student", "Receptionist", "Hungry Student")
        val varsityRoles = mutableListOf("Janitor", "Receptionist", "Security Guard", "Loud Tenant", "Maintenance Worker", "Role6", "Role7")
        val chapelRoles = mutableListOf("Janitor", "Choir Member", "Preacher", "Bride", "Groom", "Bride", "Groom")
        val colors = mutableListOf("#bf0000","#d97b00","#0f0f0f","#00d9d9","#0045d9","#7000d9","#ce00d9","#6b6b6b")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting)

        //initalizing variables
        grdPlayers = findViewById(R.id.players_grid)
        lstChatLog = findViewById(R.id.chat_log)
        edtEditMessage = findViewById(R.id.edit_message)
        btnChatClose = findViewById(R.id.close_chat)
        btnStart = findViewById(R.id.begin)
        btnLeave = findViewById(R.id.leave)
        txtRoomName = findViewById(R.id.room_name)
        chatLayout = findViewById(R.id.chat_layout)

        roomID = intent.getStringExtra("ROOM_ID").toString()
        playerID = intent.getStringExtra("PLAYER_ID").toString()
        playerName = intent.getStringExtra("PLAYER_NAME").toString()

        //connecting the location to their roles
        roles = HashMap<String, MutableList<String>>()
        roles["The Stamp"] = stampRoles
        roles["Iribe"] = iribeRoles
        roles["Capital One Field"] = fieldRoles
        roles["Eppley"] = eppleyRoles
        roles["McKeldin"] = libraryRoles
        roles["Clarice"] = clariceRoles
        roles["Marathon Deli"] = deliRoles
        roles["Dining Hall"] = diningRoles
        roles["The Varsity"] = varsityRoles
        roles["Memorial Chapel"] = chapelRoles

        //initalizing players and firebase refernces
        players = ArrayList()
        databaseRoom = FirebaseDatabase.getInstance().getReference("rooms").child(roomID)
        databaseRoomPlayers = databaseRoom.child("players")
        databaseRoomChatLog = databaseRoom.child("messages")

        //leave button onClickListener
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

        //start button onClickListener
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

        //closing chat
        btnChatClose.setOnClickListener{
            chatLayout.setBackgroundColor(Color.parseColor("#00000000"))
            var params = chatLayout.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.BELOW, R.id.actions)
            chatLayout.layoutParams = params
            var mgr = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            mgr.hideSoftInputFromWindow(chatLayout.windowToken, 0)
            edtEditMessage.clearFocus()

            btnChatClose.visibility = View.GONE
        }

        // for bringing up the chat log
        edtEditMessage.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                chatLayout.setBackgroundColor(Color.parseColor("#AAF0F0F0"))
                var params = chatLayout.layoutParams as RelativeLayout.LayoutParams
                params.addRule(RelativeLayout.BELOW, R.id.header)
                chatLayout.layoutParams = params
                btnChatClose.visibility = View.VISIBLE
            }
        }

        // for manipulating the keyboard to change the 'shift' button to 'send'
        // from https://developer.android.com/training/keyboard-input/style#Action
        edtEditMessage.setOnEditorActionListener { v, actionId, event ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    val msg = edtEditMessage.text.toString()
                    sendMessage(playerName, msg)
                    edtEditMessage.setText("")
                    true
                }
                else -> false
            }
        }

        sendMessage("", playerName + " has joined the game!")
    }

    //send messages into chat
    private fun sendMessage(sender : String, content : String) {
        // make sure message is not blank
        if (content != "") {
            val msg = ChatMessage(sender, content)
            databaseRoomChatLog.push().setValue(msg)
        }
    }

    //intent that send users ti Play.kt
    private fun startGame() {
        val intent = Intent(applicationContext, Play::class.java)
        intent.putExtra("ROOM_ID", roomID)
        intent.putExtra("PLAYER_ID", playerID)
        intent.putExtra("PLAYER_NAME", playerName)
        startActivity(intent)
        finish()
    }

    //leaving the room
    private fun leaveRoom() {
        val backLobby = Intent(applicationContext, Rooms::class.java)
        backLobby.putExtra("PLAYER_ID", playerID)
        backLobby.putExtra("PLAYER_NAME", playerName)
        startActivity(backLobby)
        finish()
    }

    override fun onStart() {
        super.onStart()

        //checks for changes happening to the room firebase reference
        onChangeListenerRoom = databaseRoom.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var gameStat = false //is the game ready to start

                //checks for inGame
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

                    //when people leaves the game
                    if (postSnapshot.key == "finished"){
                        if (postSnapshot.value as Boolean){
                            databaseRoom.removeValue()
                            if (hostID != playerID) {
                                Toast.makeText(
                                    applicationContext,
                                    "Host has left the game",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            leaveRoom()
                        }
                    }
                }
                //starts the game is inGame == true
                if (gameStat){
                    startGame()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        //checks for changes happening to the chat reference
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

        //checks for changes happening to the player reference
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
    }

    //make sure the event listeners only affects users in the current activity
    override fun onPause() {
        super.onPause()
        databaseRoom.removeEventListener(onChangeListenerRoom)
        databaseRoomPlayers.removeEventListener(onChangeListenerPlayers)
        databaseRoomChatLog.removeEventListener(onChangeListenerChatLog)
    }


    //add players to the listView
    private fun populatePlayersGrid() {
        // reset layout
        grdPlayers.removeAllViews()

        for(i in players.indices) {
            val player = players[i]
            val plate = layoutInflater.inflate(R.layout.plate, null)
            var textView = plate.findViewById<TextView>(R.id.plate_text)
            // funny business
            if (player.id == playerID) {
                textView.text = player.name + " (You!)"
            } else {
                textView.text = player.name
            }

            grdPlayers.addView(plate)

        }


    }
    @Override
    override fun onBackPressed()
    {
        if (hostID == playerID){
            databaseRoom.child("finished").setValue(true)
        } else {
            // remove the player from the list of players
            databaseRoomPlayers.child(playerID).removeValue()
            leaveRoom()
            sendMessage("", playerName + " has left the game!")
        }
    }
}