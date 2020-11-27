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


class MainActivity : AppCompatActivity() {
    private lateinit var start : Button
    private lateinit var name : EditText
    private var playerName = ""
    private lateinit var dataBase : FirebaseDatabase
    private lateinit var dataRef : DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        start = findViewById(R.id.start)
        name = findViewById(R.id.name)

        dataBase = Firebase.database;
        start.setOnClickListener(View.OnClickListener {
            playerName = name.text.toString()
            name.text.clear()
            if(playerName != ""){
                start.setText("LOADING...")
                start.isEnabled = false
                dataRef = dataBase.getReference("players/$playerName")
                addEventListener()
                dataRef.setValue("")
            }
        })




    }

    private fun addEventListener() {
        dataRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(playerName != ""){
                    val preference = getSharedPreferences("PREFS", 0)
                    val edit = preference.edit()
                    edit.putString("playerName", playerName)
                    edit.apply()
                    val room = Intent(applicationContext, Rooms::class.java);
                    startActivity(room)
                    finish()
                }

            }

            override fun onCancelled(error: DatabaseError) {
                start.setText("START!")
                start.isEnabled = true

            }
        })
    }






    companion object {
        const val TAG = "SPY_FALL_DEMO"
    }
}

