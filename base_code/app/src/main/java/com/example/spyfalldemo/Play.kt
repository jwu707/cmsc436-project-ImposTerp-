package com.example.spyfalldemo

import android.annotation.SuppressLint
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
import android.os.CountDownTimer
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.core.view.get
import androidx.core.view.size
import kotlinx.android.synthetic.main.activity_play.*
import java.lang.Exception
import java.util.ArrayList
import kotlin.math.floor
import kotlin.random.Random


class Play : Activity(){

    private lateinit var databaseRoom: DatabaseReference
    private lateinit var databaseRoomChatLog: DatabaseReference
    private lateinit var databaseRoomPlayers: DatabaseReference
    private lateinit var onChangeListenerRoom : ValueEventListener
    private lateinit var onChangeListenerRoomChatLog : ValueEventListener
    private lateinit var onChangeListenerRoomPlayers : ValueEventListener
    private lateinit var lstChatLog : ListView
    private lateinit var txtRoomName : TextView
    private lateinit var btnLeave : Button
    private lateinit var edtEditMessage : EditText
    private lateinit var btnSendMessage : Button
    private lateinit var txtRole : TextView
    private lateinit var txtLocation : TextView
    private lateinit var txtTime : TextView
    private lateinit var grdLocations : GridLayout
    private lateinit var grdPlayers : GridLayout
    private lateinit var icon: ImageView
    private lateinit var votes : HashMap<String, Int>
    private lateinit var roomID : String
    private lateinit var playerID : String
    private lateinit var playerName : String
    private lateinit var location : String
    private lateinit var players : MutableList<Player>
    private lateinit var playersMap : HashMap<String, Player>
    private lateinit var hostID : String
    private lateinit var spyID : String
    private lateinit var voteID : String
    private lateinit var countDown : CountDownTimer

    private var votingThreshold : Int = 99
    private var isHost : Boolean = false
    private var isSpy : Boolean = false
    private var timing : Boolean = false
    private var time : Long = 0

    private val icons = arrayOf(R.drawable.bee, R.drawable.top_hat, R.drawable.tu_tu, R.drawable.nerd, R.drawable.pirate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        lstChatLog = findViewById(R.id.chat_log)
        txtRoomName = findViewById(R.id.room_name)
        btnLeave = findViewById(R.id.leave)
        edtEditMessage = findViewById(R.id.edit_message)
        btnSendMessage = findViewById(R.id.send_message)
        txtTime = findViewById(R.id.timer_val)

        txtRole = findViewById(R.id.role)
        txtLocation = findViewById(R.id.location)
        //icon = findViewById(R.id.role_icon)

        roomID = intent.getStringExtra("ROOM_ID").toString()
        playerID = intent.getStringExtra("PLAYER_ID").toString()
        playerName = intent.getStringExtra("PLAYER_NAME").toString()

        players = ArrayList()
        playersMap = HashMap<String, Player>()
        votes = HashMap<String, Int>()

        databaseRoom = FirebaseDatabase.getInstance().getReference("rooms").child(roomID)
        databaseRoomPlayers = databaseRoom.child("players")
        databaseRoomChatLog = databaseRoom.child("messages")

        grdLocations = findViewById(R.id.locations_grid)
        grdPlayers = findViewById(R.id.players_grid)

        btnLeave.setOnClickListener{
            leaveRoom()
        }

        btnSendMessage.setOnClickListener{
            val msg = edtEditMessage.text.toString()
            sendMessage(playerName, msg)
            edtEditMessage.setText("")
        }

        voteID = ""
        spyID = ""

        //val randIcon = icons.indices.random()
        //icon.setImageResource(icons[randIcon])
    }

    private fun sendMessage(sender : String, content : String) {
        // make sure message is not blank
        if (content != "") {
            val msg = ChatMessage(sender, content)
            databaseRoomChatLog.push().setValue(msg)
        }
    }

    private fun leaveRoom() {
        if (hostID == playerID){
            databaseRoom.child("finished").setValue(true)
        }else {
            // remove the player from the list of players
            databaseRoomPlayers.child(playerID).removeValue()
            sendMessage("", playerName + " has left the game!")
        }
        val backLobby = Intent(applicationContext, Rooms::class.java)
        backLobby.putExtra("PLAYER_ID", playerID)
        backLobby.putExtra("PLAYER_NAME", playerName)
        startActivity(backLobby)
    }

    @SuppressLint("Range")
    private fun populatePlayersGrid() {
        // reset layout
        grdPlayers.removeAllViews()
        for(i in players.indices) {
            val player = players[i]
            val plate = layoutInflater.inflate(R.layout.plate, null)
            var textView = plate.findViewById<TextView>(R.id.plate_text)
            textView.text = player.name
            val color = player.color
            textView.setBackgroundColor(Color.parseColor(color))
            plate.id = i
            if (player.id != playerID) {
                plate.setOnClickListener { txtPlayerOnClick(i) }
            }
            grdPlayers.addView(plate)
        }
    }

    private fun txtPlayerOnClick(index : Int) {
        // indices between view id and player index in players array are offset
        val plate = grdPlayers[index]
        val textView = plate.findViewById<TextView>(R.id.plate_text)
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.generic_confirm_dialog, null)
        dialogBuilder.setView(dialogView)

        val message = dialogView.findViewById<View>(R.id.message) as TextView
        val btnYes = dialogView.findViewById<View>(R.id.yes) as Button
        val other = players[index]

        if (voteID == other.id) {
            message.text = "Are you sure you want to CANCEL your vote against " + textView.text.toString() + "?"
        } else {
            message.text = "Vote " + textView.text.toString() + " as the spy?"
        }


        val b = dialogBuilder.create()
        btnYes.setOnClickListener{

            if (voteID == other.id) { // if click confirm on player that was already voted for, cancel vote
                voteID = ""
                databaseRoomPlayers.child(playerID).child("vote").setValue("")
                sendMessage("", playerName + " canceled their vote against " + other.name + "!")
            } else {
                voteID = other.id
                databaseRoomPlayers.child(playerID).child("vote").setValue(other.id)
                sendMessage("", playerName + " votes that " + other.name + " is the spy!")
            }
            b.dismiss()
        }
        b.show()
    }

    private fun populateLocationsGrid() {

        grdLocations.removeAllViews()

        for(i in Round.locations.indices) {
            val plate = layoutInflater.inflate(R.layout.plate, null)
            val textView = plate.findViewById<TextView>(R.id.plate_text)
            textView.text = Round.locations[i]

            plate.id = i
            // only allow the spy to click on locations and guess
            if (isSpy) {
                plate.setOnClickListener { txtLocationOnClick(i) }
            }

            grdLocations.addView(plate)
        }
    }

    private fun txtLocationOnClick(index : Int) {
        val plate = grdLocations[index]
        val textView = plate.findViewById<TextView>(R.id.plate_text)
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.generic_confirm_dialog, null)
        dialogBuilder.setView(dialogView)

        val message = dialogView.findViewById<View>(R.id.message) as TextView
        val btnYes = dialogView.findViewById<View>(R.id.yes) as Button

        message.text = "Guess " + textView.text.toString() + "?"

        val b = dialogBuilder.create()
        btnYes.setOnClickListener{
            val guess = Round.locations[index]
            sendMessage("", "The spy has guessed the location: $guess")
            guessLocation(guess)
            b.dismiss()
        }
        b.show()
    }

    private fun guessLocation(guess : String) {
        if (guess == location) {
            // spy WINS
            databaseRoom.child("spyWins").setValue(true)
        } else {
            // spy LOSES
            databaseRoom.child("civilianWins").setValue(true)
        }
        databaseRoom.child("inGame").setValue(false)
        sendMessage("", playerName + " was the spy!")
    }

    private fun lookAtVotes() {

        // re-calculate voting threshold to account for players leaving
        votingThreshold = 0//floor((players.size/2).toDouble()).toInt()

        for (i in votes) {
            // check the number of votes towards every player in the game
            // if that number is greater than half the players in the game
            if (i.value > votingThreshold) {
                val player = playersMap[i.key] as Player
                sendMessage("", player.name + " was voted as the spy!")
                if (i.key == spyID) {
                    // civilians WIN
                    databaseRoom.child("civilianWins").setValue(true)
                } else {
                    // civilians LOSE
                    databaseRoom.child("spyWins").setValue(true)
                }
                sendMessage("", player.name + " was the spy!")

                databaseRoom.child("inGame").setValue(false)
            }
        }
    }

    private fun spyWinsAlert(win : Boolean) {

        // disable so they cannot be pressed
        val header = findViewById<RelativeLayout>(R.id.header)
        header.visibility = View.GONE

        val body = findViewById<RelativeLayout>(R.id.body)
        body.visibility = View.GONE

        val overlay = findViewById<RelativeLayout>(R.id.overlay)
        overlay.visibility = View.VISIBLE
        val splashart = findViewById<ImageView>(R.id.splashart)
        splashart.setImageResource(if (win) R.drawable.spy_win else R.drawable.terps_win)
        val lobbyReturnCounter = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // unimplemented
            }
            override fun onFinish() {
                backToLobby()
            }
        }
        lobbyReturnCounter.start()

        if (isHost) {
            countDown.cancel()
        }
    }

    private fun backToLobby() {
        val intent = Intent(applicationContext, Round::class.java)
        intent.putExtra("ROOM_ID", roomID)
        intent.putExtra("PLAYER_ID", playerID)
        intent.putExtra("PLAYER_NAME", playerName)
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()

        // add players from database to players array list
        onChangeListenerRoomPlayers = databaseRoomPlayers.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                // clear players and votes list
                players.clear()
                votes.clear()

                var player: Player? = null
                for (postSnapshot in dataSnapshot.children) {
                    player = postSnapshot.getValue(Player::class.java)
                    players.add(player!!)
                    playersMap[player.id] = player
                    votes[player.id] = 0

                    if (player.id == playerID) {
                        if (player.role == "Spy"){
                            txtRole.text = "You are the SPY."
                            //icon.setImageResource(R.drawable.spy)
                        } else {
                            txtRole.text = "Role: " + player.role
                        }
                    }
                }

                // iterate again to count votes
                for (postSnapshot in dataSnapshot.children) {
                    player = postSnapshot.getValue(Player::class.java)
                    if (player!!.vote != "") {
                        val count = votes[player!!.vote]!! + 1
                        votes[player!!.vote] = count
                    }
                }

                populatePlayersGrid()

                if (isHost) {
                    lookAtVotes()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        onChangeListenerRoomChatLog = databaseRoomChatLog.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var messages: MutableList<ChatMessage> = ArrayList()
                var message: ChatMessage? = null
                for (postSnapshot in dataSnapshot.children) {
                    message = postSnapshot.getValue(ChatMessage::class.java)
                    messages.add(message!!)
                }
                var adapter = ChatLogAdapter(this@Play, messages)
                lstChatLog.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        onChangeListenerRoom = databaseRoom.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                for (postSnapshot in dataSnapshot.children) {
                    // spy gets precendent in the event that there is input for a vote against
                    // the spy and a location guess at the exact same time (unlikely)
                    if (postSnapshot.key == "spyWins" && postSnapshot.value as Boolean) {
                        spyWinsAlert(true)
                    }
                    if (postSnapshot.key == "civilianWins" && postSnapshot.value as Boolean) {
                        spyWinsAlert(false)
                    }
                    if (postSnapshot.key == "host") {
                        hostID = postSnapshot.value.toString()
                        if (postSnapshot.value == playerID) {
                            isHost = true
                        }
                    }
                    if (postSnapshot.key == "spy") {
                        if (postSnapshot.value == playerID) {
                            isSpy = true
                        }
                        spyID = postSnapshot.value.toString()
                    }
                    if (postSnapshot.key == "name") {
                        txtRoomName.text = postSnapshot.value.toString()
                    }
                    if (postSnapshot.key == "location") {
                        location = postSnapshot.value.toString()
                    }
                    if (postSnapshot.key == "finished"){
                        if (postSnapshot.value == true){
                            leaveRoom()
                            Toast.makeText(applicationContext, "Host has left the game", Toast.LENGTH_SHORT).show()
                        }
                    }
                    if (postSnapshot.key == "time") {
                        time = postSnapshot.value as Long
                    }
                    if (postSnapshot.key == "timeRemaining") {
                        val ms = postSnapshot.value as Long
                        val min = ms / 60000
                        val sec = (ms % 60000) / 1000
                        txtTime.text = String.format("%d:%02d", min, sec)
                    }
                }

                if (isSpy) {
                    txtLocation.text = "Guess the Location to WIN!"
                } else {
                    txtLocation.text = "Location: " + location
                }
                // start timer
                if (isHost && !timing) {
                    time = (time*60000)
                    countDown = object : CountDownTimer(time, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            databaseRoom.child("timeRemaining").setValue(millisUntilFinished)
                        }
                        override fun onFinish() {
                            databaseRoom.child("spyWins").setValue(true)
                        }
                    }
                    countDown.start()
                    timing = true
                }

                populateLocationsGrid()
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    override fun onPause() {
        super.onPause()
        databaseRoom.removeEventListener(onChangeListenerRoom)
        databaseRoomChatLog.removeEventListener(onChangeListenerRoomChatLog)
        databaseRoomPlayers.removeEventListener(onChangeListenerRoomPlayers)
    }

    @Override
    override fun onBackPressed()
    {
        leaveRoom()
    }
}