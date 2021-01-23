package com.example.timeplayer

import android.content.*
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import com.example.timeplayer.databinding.ActivityMainBinding
import com.example.timeplayer.service.MediaService
import com.example.timeplayer.service.MediaService.Companion.ACTION_STATE_CHANGED
import com.example.timeplayer.service.MediaService.Companion.INTENT_CURRENT_PLAY
import com.example.timeplayer.service.MediaService.Companion.INTENT_CURRENT_TIME
import com.example.timeplayer.service.MediaService.Companion.INTENT_SONG_FINISHED
import com.example.timeplayer.service.MediaService.Companion.TIMER_BROADCAST

class MainActivity : AppCompatActivity() {
    private lateinit var mainBinding: ActivityMainBinding
    private var mService: Messenger? = null
    private var mServiceBound = false
    private lateinit var mBoundServiceIntent: Intent
    private lateinit var intentFilter: IntentFilter

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
            mServiceBound = false
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mService = Messenger(service)
            mServiceBound = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        mainBinding.imageButton.setBackgroundResource(R.drawable.ic_start)
        mainBinding.imageButton.setOnClickListener { onPlay() }

        mBoundServiceIntent = Intent(this@MainActivity, MediaService::class.java)
        mBoundServiceIntent.action = MediaService.ACTION_CREATE

        startService(mBoundServiceIntent)
        bindService(mBoundServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)

        setupReceiver()
    }

    private fun onPlay() {
        if (mServiceBound) {
            try {
                mService?.send(Message.obtain(null, MediaService.MESSENGER_PLAY, 0, 0))
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    private fun setPlayButton(isPlay: Boolean) {
        if (isPlay) {
            mainBinding.imageButton.setBackgroundResource(R.drawable.ic_pause)
        } else {
            mainBinding.imageButton.setBackgroundResource(R.drawable.ic_resume)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
        unregisterReceiver(broadcastReceiver)
        mBoundServiceIntent.action = MediaService.ACTION_DESTROY

        startService(mBoundServiceIntent)
    }

    fun setupReceiver() {
        intentFilter = IntentFilter();
        intentFilter.addAction(ACTION_STATE_CHANGED)
        registerReceiver(broadcastReceiver, intentFilter)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent != null) {
                if (intent.action == ACTION_STATE_CHANGED) {
                    if (intent.hasExtra(INTENT_CURRENT_TIME)) {
                        val time = intent.getStringExtra(INTENT_CURRENT_TIME) as String
                        mainBinding.textTime.text = time

                    } else if (intent.hasExtra(INTENT_SONG_FINISHED)) {
                        if (intent.getBooleanExtra(INTENT_SONG_FINISHED, false)) {
                            mainBinding.imageButton.setBackgroundResource(R.drawable.ic_start)
                        }
                    } else if (intent.hasExtra(INTENT_CURRENT_PLAY)) {
                        val isPlay = intent.getBooleanExtra(INTENT_CURRENT_PLAY, false)
                        setPlayButton(isPlay)
                    }
                }
            }

        }
    }
}