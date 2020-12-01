package com.example.spyfalldemo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.view.get
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import java.lang.Exception


class MainActivity : AppCompatActivity() {
    private lateinit var btnStart : Button
    private lateinit var edtName : EditText
    private lateinit var databasePlayers : DatabaseReference
    private var playerID : String = ""
    private var playerName : String = ""
    private lateinit var listen : ValueEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar!!.hide()

        btnStart = findViewById(R.id.start)
        edtName = findViewById(R.id.name)

        databasePlayers = FirebaseDatabase.getInstance().getReference("players")

        btnStart.setOnClickListener{ btnStartPress() }
    }

    private fun btnStartPress() {
        // set player name and clear EditText view
        var username = edtName.text.toString()
        edtName.text.clear()

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

       listen = databasePlayers.addValueEventListener(object : ValueEventListener {

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
        databasePlayers.addListenerForSingleValueEvent(listen)

    }

    override fun onDestroy() {
        super.onDestroy()
        databasePlayers.removeEventListener(listen)
    }

    companion object {
        const val TAG = "SPY_FALL_DEMO"
    }
}

