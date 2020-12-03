package com.example.spyfalldemo

import android.annotation.SuppressLint
import android.content.Context
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


class MainActivity : AppCompatActivity() {
    private lateinit var btnStart : Button
    private lateinit var edtName : EditText
    private lateinit var databasePlayers : DatabaseReference
    private var playerID : String = ""
    private var playerName : String = ""
    private lateinit var listen : ValueEventListener


    private lateinit var mFrame: FrameLayout


    // Display dimensions
    private var mDisplayWidth: Int = 0
    private var mDisplayHeight: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar!!.hide()

        btnStart = findViewById(R.id.start)
        edtName = findViewById(R.id.name)

        databasePlayers = FirebaseDatabase.getInstance().getReference("players")

        btnStart.setOnClickListener{ btnStartPress()}

        mFrame = (findViewById(R.id.frame) as FrameLayout)
    }


    private fun btnStartPress() {
        // set player name and clear EditText view
        var username = edtName.text.toString()
        edtName.text.clear()

        if (username != "") {
            btnStart.text = "LOADING..."
            btnStart.isEnabled = false

            val id = databasePlayers.push().key
            val player = Player(id!!, username)
            databasePlayers.child(id).setValue(player)

            playerID = id.toString()
            playerName = username

        } else {
            Toast.makeText(
                this,
                "Please enter a username!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {

            // Get the size of the display so this View knows where borders are
            mDisplayWidth = mFrame!!.width
            mDisplayHeight = mFrame!!.height

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

            for (i in 0 until charaArray.size){
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

    override fun onStart() {
        super.onStart()

       listen = databasePlayers.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (playerID != "") {
                    val intent = Intent(applicationContext, Rooms::class.java)
                    intent.putExtra("PLAYER_ID", playerID)
                    intent.putExtra("PLAYER_NAME", playerName)
                    startActivity(intent)
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                btnStart.text = "START!"
                btnStart.isEnabled = true
            }

        })
        databasePlayers.addListenerForSingleValueEvent(listen)

    }

    override fun onDestroy() {
        super.onDestroy()
        databasePlayers.removeEventListener(listen)
    }



    inner class CharaView internal constructor(context: Context, x: Float, y: Float, bitmap: Bitmap) :
        View(context) {
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
            val r = Random()

            createScaledBitmap(r, bitmap)

            mRadius = (mScaledBitmapWidth / 2).toFloat()
            mRadiusSquared = mRadius * mRadius

            mXPos = x - mRadius
            mYPos = y - mRadius


            setSpeedAndDirection(r)
            setRotation(r)

            mPainter.isAntiAlias = true

        }
        private fun setRotation(r: Random) {
            mDRotate = ((r.nextInt(8 * BITMAP_SIZE) + 1) / mScaledBitmapWidth).toLong()
        }

        private fun setSpeedAndDirection(r: Random) {
            mDx = (r.nextInt(mScaledBitmapWidth * 2) + 1) / mScaledBitmapWidth.toFloat()
            mDx *= (if (r.nextInt() % 2 == 0) 1 else -1).toFloat()

            mDy = (r.nextInt(mScaledBitmapWidth * 2) + 1) / mScaledBitmapWidth.toFloat()
            mDy *= (if (r.nextInt() % 2 == 0) 1 else -1).toFloat()

        }

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

        fun stop(bitmap: Bitmap) {

            if (null != mMoverFuture) {

                if (!mMoverFuture!!.isDone) {
                    mMoverFuture!!.cancel(true)
                }

                mFrame!!.post {
                    mFrame!!.removeView(this@CharaView)
                    val location = arrayOf(CharaView(mFrame!!.context, Random().nextInt(mDisplayWidth).toFloat(), 0.toFloat(), bitmap),
                        CharaView(mFrame!!.context, -Random().nextInt(mDisplayWidth).toFloat(), 0.toFloat(), bitmap),
                        CharaView(mFrame!!.context, 0.toFloat(), Random().nextInt(mDisplayHeight).toFloat(), bitmap),
                        CharaView(mFrame!!.context, 0.toFloat(), -Random().nextInt(mDisplayHeight).toFloat(), bitmap))

                    val rand = (0 until 4).random()
                    var back = location[rand]
                    mFrame!!.addView(back)
                    back.start(bitmap)

                }
            }
        }



        @Synchronized
        fun deflect(velocityX: Float, velocityY: Float) {
            mDx = velocityX / REFRESH_RATE
            mDy = velocityY / REFRESH_RATE
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

            // Move the BubbleView

            mXPos += mDx
            mYPos += mDy

            return !isOutOfView

        }
    }








    companion object {
        const val TAG = "SPY_FALL_DEMO"
    }
}

