package com.example.spyfalldemo

//creates a player class that holds all the information in which a player requires which sets the bases for what is stored under players in firebase
data class Player (val id : String = "", val name : String = "", val role : String = "", val vote : String = "")