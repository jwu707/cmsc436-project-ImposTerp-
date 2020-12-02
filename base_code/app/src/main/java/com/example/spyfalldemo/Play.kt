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

    companion object {
        //!!!!!!!!!!!make sure the role sizes are consistent! right now there's FIVE roles!!!!!!!!!!!!!!!//
        const val roleSize = 5
        val locations = arrayOf("Beach", "School", "Airport", "Park", "Gym")
        val beachRoles = arrayOf("Lifeguard", "Tourists", "Sea Monster", "Child", "Food Vendor")
        val schoolRoles = arrayOf("Student", "Teacher", "Janitor", "Librarian", "Principal")
        val airportRoles = arrayOf("Security", "Flight Attendant", "Pilot", "Crying Baby", "Lost Child")
        val parkRoles = arrayOf("Dog", "Tree", "Bench", "Photographer", "Painter")
        val gymRoles = arrayOf("Body Builder", "Trainer", "Weak Potato", "Yoga Instructor", "Boxer")
    }

    private lateinit var databaseRoom: DatabaseReference
    private lateinit var databaseRoomPlayers: DatabaseReference
    private lateinit var txtRole : TextView
    private lateinit var txtLocation : TextView
    private lateinit var grdLocations : GridLayout
    private lateinit var grdPlayers : GridLayout

    private var roles = HashMap<String, Array<String>> ()
    private var votes = HashMap<String, Int>()
    private lateinit var roomID : String
    private lateinit var playerID : String
    private lateinit var playerName : String
    private lateinit var location : String
    private var randRole = 0
    private var votingThreshold : Int = 99

    private lateinit var players : MutableList<Player>
    private lateinit var spyID : String
    private var isHost : Boolean = false
    private var isSpy : Boolean = false

    private lateinit var btnLeave: Button
    private lateinit var databaseRoomLeave: DatabaseReference
    private var host = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        txtRole = findViewById(R.id.role)
        txtLocation = findViewById(R.id.location)
        btnLeave = findViewById(R.id.leave)

        roles["Beach"] = beachRoles
        roles["School"] = schoolRoles
        roles["Airport"] = airportRoles
        roles["Park"] = parkRoles
        roles["Gym"] = gymRoles

        roomID = intent.getStringExtra("ROOM_ID").toString()
        playerID = intent.getStringExtra("PLAYER_ID").toString()
        playerName = intent.getStringExtra("PLAYER_NAME").toString()
        host = intent.getStringExtra("HOST").toString()

        players = ArrayList()

        databaseRoom = FirebaseDatabase.getInstance().getReference("rooms").child(roomID)
        databaseRoomPlayers = databaseRoom.child("players")

        grdLocations = findViewById(R.id.locations_grid)
        grdPlayers = findViewById(R.id.players_grid)


//        btnLeave.setOnClickListener(View.OnClickListener {
//            if (host == playerID){
//                databaseRoom.child("finished").setValue(true)
//            }else {
//                databaseRoomLeave = databaseRoomPlayers.child(playerID)
//                databaseRoomLeave.removeValue()
//                val backLobby = Intent(applicationContext, Rooms::class.java)
//                backLobby.putExtra("PLAYER_ID", playerID)
//                backLobby.putExtra("PLAYER_NAME", playerName)
//                startActivity(backLobby)
//            }
//        })
    }

    private fun populatePlayersGrid() {

        // reset layout
        grdPlayers.removeAllViews()

        for(i in players.indices) {
            val player = players[i]

            // only add player names that are not this player

            val plate = layoutInflater.inflate(R.layout.plate, null)
            var textView = plate.findViewById<TextView>(R.id.plate_text)
            textView.text = player.name

            plate.id = i

            if (player.id != playerID) {
                plate.setOnClickListener { txtPlayerOnClick(i) }
            } else {
                plate.visibility = View.GONE
            }

            grdPlayers.addView(plate)

        }
    }

    private fun txtPlayerOnClick(index : Int) {
        // indices between view id and player index in players array are offset
        Log.i("Play", "Clicked player index: $index")
        val plate = grdPlayers[index]
        val textView = plate.findViewById<TextView>(R.id.plate_text)
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.generic_confirm_dialog, null)
        dialogBuilder.setView(dialogView)

        val message = dialogView.findViewById<View>(R.id.message) as TextView
        val btnYes = dialogView.findViewById<View>(R.id.yes) as Button

        message.text = "Vote " + textView.text.toString() + " as the spy?"

        btnYes.setOnClickListener{
            val other = players[index]
            databaseRoomPlayers.child(playerID).child("vote").setValue(other.id)
        }

        val b = dialogBuilder.create()
        b.show()
    }

    private fun populateLocationsGrid() {

        grdLocations.removeAllViews()

        for(i in locations.indices) {
            val plate = layoutInflater.inflate(R.layout.plate, null)
            val textView = plate.findViewById<TextView>(R.id.plate_text)
            textView.text = locations[i]

            plate.id = i
            // only allow the spy to click on locations and guess
            if (isSpy) {
                plate.setOnClickListener { txtLocationOnClick(i) }
            }

            grdLocations.addView(plate)
        }
    }

    private fun txtLocationOnClick(index : Int) {
        Log.i("Play", "Clicked location index: $index")
        val plate = grdLocations[index]
        val textView = plate.findViewById<TextView>(R.id.plate_text)
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.generic_confirm_dialog, null)
        dialogBuilder.setView(dialogView)

        val message = dialogView.findViewById<View>(R.id.message) as TextView
        val btnYes = dialogView.findViewById<View>(R.id.yes) as Button

        message.text = "Guess " + textView.text.toString() + "?"

        btnYes.setOnClickListener{
            guessLocation(locations[index])
        }

        val b = dialogBuilder.create()
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
    }

    private fun lookAtVotes() {

        // re-calculate voting threshold to account for players leaving
        votingThreshold = floor((players.size/2).toDouble()).toInt()

        for (i in votes) {
            // check the number of votes towards every player in the game
            // if that number is greater than half the players in the game
            if (i.value > votingThreshold) {
                if (i.key == spyID) {
                    // civilians WIN
                    databaseRoom.child("civilianWins").setValue(true)
                } else {
                    // civilians LOSE
                    databaseRoom.child("spyWins").setValue(true)
                }
                databaseRoom.child("inGame").setValue(false)
            }
        }
    }

    private fun spyWinsAlert() {
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.generic_confirm_dialog, null)
        dialogBuilder.setView(dialogView)

        val message = dialogView.findViewById<View>(R.id.message) as TextView
        val btnYes = dialogView.findViewById<View>(R.id.yes) as Button

        message.text = "The spy wins!"
        btnYes.text = "Back to Lobby"

        btnYes.setOnClickListener{
            backToLobby()
        }

        val b = dialogBuilder.create()
        b.show()
    }

    private fun civilianWinsAlert() {
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.generic_confirm_dialog, null)
        dialogBuilder.setView(dialogView)

        val message = dialogView.findViewById<View>(R.id.message) as TextView
        val btnYes = dialogView.findViewById<View>(R.id.yes) as Button

        message.text = "The civilians win!"
        btnYes.text = "Back to Lobby"

        btnYes.setOnClickListener{
            backToLobby()
        }

        val b = dialogBuilder.create()
        b.show()
    }

    private fun backToLobby() {
        val intent = Intent(applicationContext, Round::class.java)
        intent.putExtra("ROOM_ID", roomID)
        intent.putExtra("PLAYER_ID", playerID)
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()

        // add players from database to players array list
        databaseRoomPlayers.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                // clear players and votes list
                players.clear()
                votes.clear()

                var player: Player? = null
                for (postSnapshot in dataSnapshot.children) {
                    try {
                        player = postSnapshot.getValue(Player::class.java)

                    } catch (e: Exception) {

                    } finally {
                        players.add(player!!)
                        votes[player.id] = 0
                    }
                }

                // iterate again to count votes
                for (postSnapshot in dataSnapshot.children) {
                    try {
                        player = postSnapshot.getValue(Player::class.java)
                    } catch (e: Exception) {

                    } finally {
                        if (player!!.vote != "") {
                            val count = votes[player!!.vote]!! + 1
                            votes[player!!.vote] = count
                        }
                    }
                }

                populatePlayersGrid()

                if (isHost) {
                    lookAtVotes()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        databaseRoom.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (postSnapshot in dataSnapshot.children) {
                    try {
                        // spy gets precendent in the event that there is input for a vote against
                        // the spy and a location guess at the exact same time (unlikely)
                        if (postSnapshot.key == "spyWins") {
                            if (postSnapshot.value as Boolean) {
                                spyWinsAlert()
                            }
                        }
                        if (postSnapshot.key == "civilianWins") {
                            if (postSnapshot.value as Boolean) {
                                civilianWinsAlert()
                            }
                        }
                        if (postSnapshot.key == "host") {
                            if (postSnapshot.value == playerID) {
                                isHost = true
                            }
                        }
                        if (postSnapshot.key == "spy") {
                            if (postSnapshot.value == playerID) {
                                isSpy = true
                            }
                        }
                        if (postSnapshot.key == "location") {
                            location = postSnapshot.value.toString()
                        }

//                        if (postSnapshot.key == "finished"){
//                            if (postSnapshot.value as Boolean){
//                                databaseRoom.removeValue()
//                                Toast.makeText(applicationContext, "Host has left the game", Toast.LENGTH_SHORT).show()
//                                val backLobby = Intent(applicationContext, Rooms::class.java)
//                                backLobby.putExtra("PLAYER_ID", playerID)
//                                backLobby.putExtra("PLAYER_NAME", playerName)
//                                startActivity(backLobby)
//                            }
//                        }

                        randRole = (0 until roleSize).random()
                        // duplicate roles
                        if (isSpy){
                            txtRole.text = "You are the SPY."
                            txtLocation.text = "Guess the Location to WIN!"
                        }else{
                            txtRole.text = "Role: " + roles[location]!![randRole].toString()
                            txtLocation.text = "Location: " + location
                        }
                    } catch (e: Exception) {

                    } finally {

                    }
                }
                populateLocationsGrid()
            }
            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }
}