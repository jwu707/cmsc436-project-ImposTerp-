package com.example.spyfalldemo

data class Room(val id : String = "", val name : String = "", val maxPlayers : Int = 10, val host : String = "", val spy : String = "", val inGame : Boolean = false, val players : MutableList<Player> = ArrayList())