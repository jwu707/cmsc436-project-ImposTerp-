package com.example.spyfalldemo

// creates a room class which sets the information in which a room requires and it sets the bases for what is then stored under rooms in firbase
data class Room(
    val id : String = "",
    val name : String = "",
    val password : String = "",
    val maxPlayers : Int = 10,
    val time : Int = 1,
    val host : String = "",
    val spy : String = "",
    val location : String = "",
    val inGame : Boolean = false,
    val spyWins : Boolean = false,
    val civilianWins : Boolean = false,
    val players : HashMap<String, Player> = HashMap<String, Player>(),
    val messages : HashMap<String, ChatMessage> = HashMap<String, ChatMessage>()
    )