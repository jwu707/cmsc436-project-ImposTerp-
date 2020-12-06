package com.example.spyfalldemo

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class RoomListAdapter(private val context: Activity, private var rooms: List<Room>) : ArrayAdapter<Room>(context,
    R.layout.list_item_room, rooms) {

    @SuppressLint("InflateParams", "ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val listViewItem = inflater.inflate(R.layout.list_item_room, null, true)

        val txtRoomName = listViewItem.findViewById<View>(R.id.name) as TextView
        val txtPlayers = listViewItem.findViewById<View>(R.id.players) as TextView
        val txtStatus = listViewItem.findViewById<View>(R.id.status) as TextView
        val txtTime = listViewItem.findViewById<View>(R.id.time) as TextView
        val txtPassword = listViewItem.findViewById<View>(R.id.password) as TextView

        val room = rooms[position]
        txtRoomName.text = room.name
        txtPlayers.text = room.players.size.toString() + " / " + room.maxPlayers
        txtStatus.text = if (room.inGame) "In Game" else "In Lobby"
        txtTime.text = "Discussion Time: " + room.time + " minutes"

        val ps = room.password
        if (ps == "") {
            txtPassword.visibility = View.GONE
        }

        return listViewItem
    }
}