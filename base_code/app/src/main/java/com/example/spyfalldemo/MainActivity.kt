package com.example.spyfalldemo

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.get
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import java.lang.Exception
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


//The main activity is the main menu of the app in which the user input their desired username and and ID made by firebase will be used to store information about the user
//The credit button will give information regarding the creators of the app
//the rules button will give the users rules about the app
//When the user inputs a desired username and press start they will be taken to the Rooms.kt activity

class MainActivity : AppCompatActivity() {
    //Main screen buttons
    private lateinit var btnStart : Button
    private lateinit var btnRules : Button
    private lateinit var btnCredits : Button

    //Enter Name
    private lateinit var edtName : EditText

    //Firebase references
    private lateinit var databasePlayers : DatabaseReference
    private lateinit var onChangeListenerPlayers : ValueEventListener
    private lateinit var mFrame: FrameLayout

    private lateinit var names : MutableList<String>

    private var playerID : String = ""
    private var playerName : String = ""

    // Display dimensions (referenced from lab 9 & 10)
    private var mDisplayWidth: Int = 0
    private var mDisplayHeight: Int = 0

    //no not reset characters with pop-ups
    private var noReset = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar!!.hide()

        btnStart = findViewById(R.id.start)
        edtName = findViewById(R.id.name)
        btnCredits = findViewById(R.id.credits)
        btnRules = findViewById(R.id.rules)
        names = ArrayList()

        //player firebase refernce
        databasePlayers = FirebaseDatabase.getInstance().getReference("players")

        //onClickListeners for all the main screen buttons
        btnStart.setOnClickListener{ btnStartPress()}
        btnRules.setOnClickListener{btnRulesPress()}
        btnCredits.setOnClickListener{btnCreditPress()}

        mFrame = findViewById<FrameLayout>(R.id.frame)
    }

    //Credit button pop-up
    private fun btnCreditPress() {
        noReset = false // pop-up will not reset the chara

        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.generic_confirm_dialog, null)
        dialogBuilder.setView(dialogView)

        val txtMessage = dialogView.findViewById(R.id.message) as TextView
        val btnBack = dialogView.findViewById(R.id.yes) as Button

        txtMessage.setText(R.string.credits)
        btnBack.text = "Close"

        val b = dialogBuilder.create()
        btnBack.setOnClickListener{
            b.dismiss()
        }
        b.show()
    }

    //Rule button pop-up
    private fun btnRulesPress() {
        noReset = false  // pop-up will not reset the chara

        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.generic_confirm_dialog, null)
        dialogBuilder.setView(dialogView)

        val txtMessage = dialogView.findViewById(R.id.message) as TextView
        val btnBack = dialogView.findViewById(R.id.yes) as Button

        txtMessage.setText(R.string.rules)
        txtMessage.textSize = 16F
        txtMessage.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        btnBack.text = "Close"

        val b = dialogBuilder.create()
        btnBack.setOnClickListener{
            b.dismiss()
        }
        b.show()
    }

    //Start button function
    private fun btnStartPress() {
        // set player name and clear EditText view
        var username = edtName.text.toString()

        if (username != "") {
            if(username.length <= 16) {
                if (!names.contains(username)) {
                    //Make a player id from fierbase
                    val id = databasePlayers.push().key
                    //create a player
                    val player = Player(id!!, username)
                    //set the player info
                    databasePlayers.child(id).setValue(player)

                    playerID = id.toString()
                    playerName = username
                } else {
                    Toast.makeText(
                        this,
                        "That name is unavailable!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    this,
                    "That name is too long!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else { // name cannot be blank
            Toast.makeText(
                this,
                "Please enter a username!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onStart() {
        super.onStart()

        //firbase player refernce checks for changes to its data
        onChangeListenerPlayers = databasePlayers.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                names.clear()
                for (player in dataSnapshot.children) {
                    for (property in player.children) {
                        if (property.key == "name") {
                            names.add(property.value.toString())
                        }
                    }
                }

                //after a name is inputed the user can move onto the next activity
                if (playerID != "") {
                    val intent = Intent(applicationContext, Rooms::class.java)
                    intent.putExtra("PLAYER_ID", playerID)
                    intent.putExtra("PLAYER_NAME", playerName)
                    startActivity(intent)
                        //finish()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                btnStart.isEnabled = true
            }
        })
    }

    //make sure the event listener will only change users on the current activity
    override fun onPause() {
        super.onPause()
        databasePlayers.removeEventListener(onChangeListenerPlayers)
    }

    //Adds the floating character element (referenced from lab 9 and 10)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && noReset) {

            //get the display paramenters
            mDisplayWidth = mFrame!!.width
            mDisplayHeight = mFrame!!.height

            //array of all the characters
            val charaArray = arrayOf( BitmapFactory.decodeResource(resources, R.drawable.bee),
                                      BitmapFactory.decodeResource(resources, R.drawable.top_hat),
                                      BitmapFactory.decodeResource(resources, R.drawable.tu_tu),
                                      BitmapFactory.decodeResource(resources, R.drawable.pirate),
                                      BitmapFactory.decodeResource(resources, R.drawable.spy),
                                      BitmapFactory.decodeResource(resources, R.drawable.nerd),
                                      BitmapFactory.decodeResource(resources, R.drawable.chef),
                                      BitmapFactory.decodeResource(resources, R.drawable.gamer),
                                      BitmapFactory.decodeResource(resources, R.drawable.football),
                                      BitmapFactory.decodeResource(resources, R.drawable.music))

            //a loop to add each one onto screen
            for (i in charaArray.indices){
                val addChara = CharaView(
                    mFrame!!.context,
                    Random().nextInt(mDisplayWidth).toFloat(),
                    Random().nextInt(mDisplayHeight).toFloat(),
                    charaArray[i]
                )
                mFrame!!.addView(addChara)
                addChara.start(charaArray[i])
            }

        }
    }

    //CharaView is refernced from lab 9 and 10, the class adds moving characters to the main screen frame with randomized size, speed, and rotation.
    //In addition, the class will replace a character that has gone out of frame with another character, in which it spawns at the edge of the screen

    //Similar to BubbleView from lab 9 and 10 with changes labled
    inner class CharaView internal constructor(context: Context, x: Float, y: Float, bitmap: Bitmap) : View(context) {
        private val mPainter = Paint()
        private var mMoverFuture: ScheduledFuture<*>? = null
        private var mScaledBitmapWidth: Int = 0
        private var mScaledBitmap: Bitmap? = null
        private val BITMAP_SIZE = 64
        private val REFRESH_RATE = 40
        private var mXPos: Float = 0.toFloat()
        private var mYPos: Float = 0.toFloat()
        private var mDx: Float = 0.toFloat()
        private var mDy: Float = 0.toFloat()
        private val mRadius: Float
        private val mRadiusSquared: Float
        private var mRotate: Long = 0
        private var mDRotate: Long = 0


        private val isOutOfView: Boolean
            get() = (mXPos < 0 - mScaledBitmapWidth || mXPos > mDisplayWidth
                    || mYPos < 0 - mScaledBitmapWidth || mYPos > mDisplayHeight)

        init {
           //creating random sized bitmap
            val r = Random()
            createScaledBitmap(r, bitmap)

            mRadius = (mScaledBitmapWidth / 2).toFloat()
            mRadiusSquared = mRadius * mRadius

            mXPos = x - mRadius
            mYPos = y - mRadius

            //instead of having selections of random/still/one speed the speed will always be random
            setSpeedAndDirection(r)
            setRotation(r)

            mPainter.isAntiAlias = true

        }

        private fun setRotation(r: Random) { //CharaView is edited so that it includes more rotation
            mDRotate = ((r.nextInt(8 * BITMAP_SIZE) + 1) / mScaledBitmapWidth).toLong()
        }

        private fun setSpeedAndDirection(r: Random) { //the speed is slowed down more
            mDx = (r.nextInt(mScaledBitmapWidth * 2) + 1) / mScaledBitmapWidth.toFloat()
            mDx *= (if (r.nextInt() % 2 == 0) 1 else -1).toFloat()

            mDy = (r.nextInt(mScaledBitmapWidth * 2) + 1) / mScaledBitmapWidth.toFloat()
            mDy *= (if (r.nextInt() % 2 == 0) 1 else -1).toFloat()

        }

        //the range for image size is also enlarged
        private fun createScaledBitmap(r: Random, mBitmap: Bitmap) {
            mScaledBitmapWidth = r.nextInt(8 * BITMAP_SIZE) + BITMAP_SIZE

            mScaledBitmap = Bitmap.createScaledBitmap(
                mBitmap!!,
                mScaledBitmapWidth, mScaledBitmapWidth, false
            )
        }

        fun start(bitmap: Bitmap) {
            val executor = Executors
                .newScheduledThreadPool(1)

            mMoverFuture = executor.scheduleWithFixedDelay({

                if (moveWhileOnScreen()) {
                    postInvalidate()
                } else
                    stop(bitmap)
            }, 0, REFRESH_RATE.toLong(), TimeUnit.MILLISECONDS)
        }



        //when a character goes out of screen a new one is added to replace the one that is out of frame
        fun stop(bitmap: Bitmap) {
            if (null != mMoverFuture) {

                if (!mMoverFuture!!.isDone) {
                    mMoverFuture!!.cancel(true)
                }

                mFrame!!.post {
                    mFrame!!.removeView(this@CharaView)

                    //the new chara is spawned back into frame at the edges of the screen
                    //this array holds all locations in which new characters can spawn
                    val location = arrayOf(CharaView(mFrame!!.context, Random().nextInt(mDisplayWidth).toFloat(), 0.toFloat(), bitmap),
                        CharaView(mFrame!!.context, -Random().nextInt(mDisplayWidth).toFloat(), 0.toFloat(), bitmap),
                        CharaView(mFrame!!.context, 0.toFloat(), Random().nextInt(mDisplayHeight).toFloat(), bitmap),
                        CharaView(mFrame!!.context, 0.toFloat(), -Random().nextInt(mDisplayHeight).toFloat(), bitmap))

                    //the array is then totally randomized
                    val rand = (0 until 4).random()
                    var back = location[rand]
                    mFrame!!.addView(back)
                    back.start(bitmap)

                }
            }
        }

        @Synchronized
        override fun onDraw(canvas: Canvas) {
            canvas.save()

            mRotate += mDRotate

            canvas.rotate(
                mRotate.toFloat(),
                mXPos + mScaledBitmapWidth / 2,
                mYPos + mScaledBitmapWidth / 2
            )

            canvas.drawBitmap(mScaledBitmap!!, mXPos, mYPos, mPainter)
            canvas.restore()

        }

        @Synchronized
        private fun moveWhileOnScreen(): Boolean {
            mXPos += mDx
            mYPos += mDy

            return !isOutOfView

        }
    }
}

