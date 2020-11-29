package com.example.spyfalldemo

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import java.lang.Exception


class MainActivity : AppCompatActivity() {
    private lateinit var btnStart : Button
    private lateinit var txtName : EditText
    private lateinit var databasePlayers : DatabaseReference
    private var playerID : String = ""
    private var playerName : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStart = findViewById(R.id.start)
        txtName = findViewById(R.id.name)

        databasePlayers = FirebaseDatabase.getInstance().getReference("players")

        btnStart.setOnClickListener{ btnStartPress() }
    }

    private fun btnStartPress() {
        // set player name and clear EditText view
        var username = txtName.text.toString()
        txtName.text.clear()

        if (username != "") {
            btnStart.text = "LOADING..."
            btnStart.isEnabled = false

            val id = databasePlayers.push().key
            val player = Player(id!!, username)
            databasePlayers.child(id).setValue(player)

            playerID = id.toString()
            playerName = username

        } else {
            Toast.makeText(
                this,
                "Please enter a username!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onStart() {
        super.onStart()

        databasePlayers.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (playerID != "") {
                    val intent = Intent(applicationContext, Rooms::class.java)
                    intent.putExtra("PLAYER_ID", playerID)
                    intent.putExtra("PLAYER_NAME", playerName)
                    startActivity(intent)
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                btnStart.text = "START!"
                btnStart.isEnabled = true
            }

        })
    }

    companion object {
        const val TAG = "SPY_FALL_DEMO"
    }
}

