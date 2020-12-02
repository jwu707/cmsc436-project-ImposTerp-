package com.example.spyfalldemo

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_play.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.set


class Play : Activity(){

    private lateinit var playerRef: DatabaseReference
    private lateinit var roomRef: DatabaseReference
    private lateinit var roleView : TextView
    private lateinit var locView : TextView

    val location = arrayOf("Beach", "School", "Airport", "Park", "Gym")

    //!!!!!!!!!!!make sure the role sizes are consistent! right now theres FIVE roles!!!!!!!!!!!!!!!//
    val roleSize = 5

    val beachRoles = arrayOf("Lifeguard", "Tourists", "Sea Monster", "Child", "Food Vendor")
    val schoolRoles = arrayOf("Student", "Teacher", "Janitor", "Librarian", "Principal")
    val airportRoles = arrayOf("Security", "Flight Attendant", "Pilot", "Crying Baby", "Lost Child")
    val parkRoles = arrayOf("Dog", "Tree", "Bench", "Photographer", "Painter")
    val gymRoles = arrayOf("Body Builder", "Trainer", "Weak Potato", "Yoga Instructor", "Boxer")



    private var roles = HashMap<String, Array<String>> ()
    private var roomID = ""
    private var playerID = ""
    private var randRole = 0
    private var loc = ""
    private var spy = ""

    private var chatList = ArrayList<Chat>()
    private lateinit var chatButton:Button
    private lateinit var sendButton:ImageButton
    private lateinit var chatLayout:FrameLayout
    private lateinit var chatMessage:EditText


    private lateinit var players : ArrayList<Player>

    override fun onCreate(savedInstanceState: Bundle?) {
        //Log.i("TAG", "create new player activity")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        roleView = findViewById(R.id.textView3)
        locView = findViewById(R.id.textView)

        chatLayout = findViewById(R.id.ChatLayout)
        chatButton = findViewById(R.id.ChatButton)
        sendButton = findViewById(R.id.sendButton)
        chatMessage = findViewById(R.id.messageText)

        var chatList = ArrayList<Chat>()


        roles["Beach"] = beachRoles
        roles["School"] = schoolRoles
        roles["Airport"] = airportRoles
        roles["Park"] = parkRoles
        roles["Gym"] = gymRoles

        roomID = intent.getStringExtra("ROOM_ID").toString()
        playerID = intent.getStringExtra("PLAYER_ID").toString()
        loc = intent.getStringExtra("LOCATION").toString()
        spy = intent.getStringExtra("SPY").toString()

        roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomID)

        chatView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)



        randRole = (0 until roleSize).random()
        if (playerID == spy){
            roleView.text = "SPY"
            locView.text = "Guess the Location to WIN!"
        }else{
            roleView.text = roles.get(loc)!![randRole].toString()
            locView.text = "Location: " + loc
        }

        chatButton.setOnClickListener {
            if (chatLayout.visibility != View.VISIBLE)
                chatLayout.visibility = View.VISIBLE
            else
                chatLayout.visibility = View.GONE

        }
        sendButton.setOnClickListener{
            sendMessage(playerID, chatMessage)
        }

        readMessage()
    }

    fun sendMessage(playerID: String, message: EditText){
        if (message.text.toString() != "") {
            var chat = Chat(playerID, chatMessage.text.toString())
            roomRef.child("chat").push().setValue(chat)
            message.text.clear()
        }
    }

    fun readMessage() {
        val databaseReference: DatabaseReference = roomRef.child("chat")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()
                for (dataSnapShot: DataSnapshot in snapshot.children) {
                    //Log.i("TAG", "dataSnapshot " + dataSnapShot.getValue(String::class.java))
                    val senderId = dataSnapShot.child("senderId").getValue(String::class.java)
                    val message = dataSnapShot.child("message").getValue(String::class.java)

                    var chat = Chat(senderId, message)

                    if (chat != null) {
                        chatList.add(chat)
                    }
                }

                val chatAdapter = ChatAdapter(this@Play, playerID, chatList, FirebaseDatabase.getInstance())
                chatView.adapter = chatAdapter
            }
        })
    }



    private fun castVote(playerID : String) {

    }

    private fun guess() {

    }
}