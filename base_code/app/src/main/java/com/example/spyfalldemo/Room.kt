package com.example.spyfalldemo

data class Room(
    val id : String = "",
    val name : String = "",
    val maxPlayers : Int = 10,
    val host : String = "",
    val spy : String = "",
    val location : String = "",
    val inGame : Boolean = false,
    val spyWins : Boolean = false,
    val civilianWins : Boolean = false,
    val players : HashMap<String, Player> = HashMap<String, Player>())