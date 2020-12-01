package com.example.spyfalldemo

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class LobbyPlayerListAdapter(private val context: Activity, private var players: List<Player>, private val host : String) : ArrayAdapter<Player>(context,
    R.layout.list_item_lobby_player, players) {

    @SuppressLint("InflateParams", "ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val listViewItem = inflater.inflate(R.layout.list_item_lobby_player, null, true)

        val textViewName = listViewItem.findViewById<View>(R.id.name) as TextView

        val player = players[position]

        if (player.id == host) {
            textViewName.text = player.name + " (Host)"
        } else {
            textViewName.text = player.name
        }

        return listViewItem
    }
}