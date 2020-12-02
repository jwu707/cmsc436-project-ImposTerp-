package com.example.spyfalldemo

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*


class ChatAdapter(private val context: Context, private val userID: String, private val chatList: ArrayList<Chat>, private val database: FirebaseDatabase):
        RecyclerView.Adapter<ChatAdapter.ViewHolder>(){

        private val MESSAGE_TYPE_LEFT = 0
        private val MESSAGE_TYPE_RIGHT = 1


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatAdapter.ViewHolder {
                if (viewType == MESSAGE_TYPE_RIGHT) {
                        val view =
                                LayoutInflater.from(parent.context).inflate(R.layout.chat_right, parent, false)
                        return ViewHolder(view)
                } else {
                        val view =
                                LayoutInflater.from(parent.context).inflate(R.layout.chat_left, parent, false)
                        return ViewHolder(view)
                }
        }

        override fun getItemCount(): Int {
                return chatList.size
        }

        override fun onBindViewHolder(holder: ChatAdapter.ViewHolder, position: Int) {
                val chat = chatList[position]
                var senderId = "Something fucked up"
                database.getReference("players").child(chat.senderId.toString()).addListenerForSingleValueEvent(
                        object : ValueEventListener {
                                override fun onCancelled(error: DatabaseError) {
                                        TODO("Not yet implemented")
                                }

                                override fun onDataChange(snapshot: DataSnapshot) {
                                        for (dataSnapShot: DataSnapshot in snapshot.children) {
                                                Log.i("Tag", "userid "+chat.senderId.toString())
                                                senderId = (snapshot.getValue(Player::class.java)?.name).toString()
                                                Log.i("Tag", "id name "+ senderId)
                                                holder.txtUserName.text = senderId
                                        }
                                }

                        }
                )
                holder.txtMessage.text = chat.message
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
                val txtUserName: TextView = view.findViewById(R.id.Username)
                val txtMessage: TextView = view.findViewById(R.id.Message)
        }

        override fun getItemViewType(position: Int): Int {
                if (chatList[position].senderId == userID) {
                        return MESSAGE_TYPE_RIGHT
                } else {
                        return MESSAGE_TYPE_LEFT
                }

        }
}