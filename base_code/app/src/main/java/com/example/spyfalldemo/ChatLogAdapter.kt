package com.example.spyfalldemo

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

//This a ChatLogAdaptor that properly displays the chat window. The code was references from the firebase lab

class ChatLogAdapter(private val context: Activity, private var messages: List<ChatMessage>) : ArrayAdapter<ChatMessage>(context,
    R.layout.list_item_chat_message, messages) {

    @SuppressLint("InflateParams", "ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val message = messages[position]

        val inflater = context.layoutInflater
        val listViewItem = inflater.inflate(R.layout.list_item_chat_message, null, true)

        val txtSender = listViewItem.findViewById(R.id.sender) as TextView
        val txtContent = listViewItem.findViewById(R.id.content) as TextView

        // for special messages from host
        if (message.sender == "") {
            txtSender.text = message.content
            txtContent.text = ""
        } else {
            txtSender.text = message.sender
            txtContent.text = message.content
        }

        return listViewItem
    }
}