package com.example.spyfalldemo

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class ChatLogAdapter(private val context: Activity, private var messages: List<ChatMessage>) : ArrayAdapter<ChatMessage>(context,
    R.layout.list_item_chat_message, messages) {

    @SuppressLint("InflateParams", "ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val listViewItem = inflater.inflate(R.layout.list_item_chat_message, null, true)

        val txtSender = listViewItem.findViewById(R.id.sender) as TextView
        val txtContent = listViewItem.findViewById(R.id.content) as TextView

        val message = messages[position]

        txtSender.text = message.sender
        txtContent.text = message.content

        return listViewItem
    }
}