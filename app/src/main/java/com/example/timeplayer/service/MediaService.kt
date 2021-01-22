package com.example.timeplayer.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import com.example.timeplayer.R
import com.example.timeplayer.util.NotificationUtil
import com.example.timeplayer.util.NotificationUtil.KEY_NOTIFICATION_ID
import java.io.IOException
import java.lang.ref.WeakReference

class MediaService : Service(), MediaPlayerCallback {
    private var isReady: Boolean = false
    private var isPlay: Boolean = false
    private var time: String = ""
    private var mMediaPlayer: MediaPlayer? = null
    lateinit var mHandler: Handler

    companion object {
        const val TIMER_BROADCAST = "com.example.timeplayer.mediaservice.timer"
        const val ACTION_CREATE = "com.example.timeplayer.mediaservice.create"
        const val ACTION_DESTROY = "com.example.timeplayer.mediaservice.destroy"
        const val ACTION_NOTIFICATION_PLAY = "action_notification_play"
        const val ACTION_STATE_CHANGED = "action_state_changed"
        const val INTENT_CURRENT_TIME = "current_time"
        const val INTENT_SONG_FINISHED = "finished"
        const val INTENT_CURRENT_PLAY = "current_play"
        const val MESSENGER_PLAY = 0
        private const val REFRESH_DELAY: Long = 1000
    }

    override fun onCreate() {
        super.onCreate()
        mHandler = Handler(Looper.getMainLooper())
        mHandler.removeCallbacks(mTimer)
        mHandler.postDelayed(mTimer, 0)

        val intentFilter = IntentFilter();
        intentFilter.addAction(ACTION_NOTIFICATION_PLAY)
        intentFilter.addAction(ACTION_STATE_CHANGED)

        registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        if (action != null) {
            when (action) {
                ACTION_CREATE -> if (mMediaPlayer == null) {
                    init()
                }
                ACTION_DESTROY -> if (mMediaPlayer?.isPlaying as Boolean) {
                    stopSelf()
                }
                else -> {
                    init()
                }
            }
        }
        return flags
    }


    override fun onBind(intent: Intent): IBinder {
        return mMessenger.binder
    }

    private fun init() {
        mMediaPlayer = MediaPlayer()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val attribute = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            mMediaPlayer?.run {
                setAudioAttributes(attribute)
            }
        } else {
            mMediaPlayer?.run { AudioManager.STREAM_MUSIC }
        }

        val afd = applicationContext.resources.openRawResourceFd(R.raw.kaze_kaoru)
        try {
            mMediaPlayer?.run { setDataSource(afd.fileDescriptor, afd.startOffset, afd.length) }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        mMediaPlayer?.setOnPreparedListener {
            isReady = true
            isPlay = true
            mMediaPlayer?.start()

            val broadcastIntent = Intent(TIMER_BROADCAST)
            broadcastIntent.action = ACTION_STATE_CHANGED
            broadcastIntent.putExtra(INTENT_CURRENT_PLAY, isPlay)
            sendBroadcast(broadcastIntent)
        }

        mMediaPlayer?.setOnErrorListener { mp, what, extra ->
            false
        }

        mMediaPlayer?.setOnCompletionListener {
            stopNotif()
            val broadcastIntent = Intent(TIMER_BROADCAST)
            broadcastIntent.action = ACTION_STATE_CHANGED
            broadcastIntent.putExtra(INTENT_SONG_FINISHED, true)
            sendBroadcast(broadcastIntent)
        }
    }

    override fun onPlay() {
        if (!isReady) {
            mMediaPlayer?.prepareAsync()
        } else {
            if (mMediaPlayer?.isPlaying as Boolean) {
                mMediaPlayer?.pause()
                mHandler.removeCallbacks(mTimer)
            } else {
                mMediaPlayer?.start()
                mHandler.postDelayed(mTimer, 0)
                showNotif()
            }
            isPlay = !isPlay
            NotificationUtil.updateNotification(this, time, isPlay)

            val broadcastIntent = Intent(TIMER_BROADCAST)
            broadcastIntent.action = ACTION_STATE_CHANGED
            broadcastIntent.putExtra(INTENT_CURRENT_PLAY, isPlay)
            sendBroadcast(broadcastIntent)
        }
    }

    override fun onDestroy() {
        mHandler.removeCallbacks(mTimer)
        unregisterReceiver(broadcastReceiver)
        stopNotif()
        super.onDestroy()
    }

    private val mMessenger = Messenger(IncomingHandler(this))

    internal class IncomingHandler(playerCallback: MediaPlayerCallback) :
        Handler(Looper.getMainLooper()) {
        private val mediaPlayerCallbackWeakReference: WeakReference<MediaPlayerCallback> =
            WeakReference(playerCallback)

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSENGER_PLAY -> mediaPlayerCallbackWeakReference.get()?.onPlay()
                else -> super.handleMessage(msg)
            }
        }
    }

    private fun updateTimer() {
        if (mMediaPlayer != null) {
            val millis = mMediaPlayer?.currentPosition as Int
            val second = (millis / 1000) % 60
            val minute = (millis / (1000 * 60)) % 60
            val hour = (millis / (1000 * 60 * 60)) % 24

            time = String.format("%02d:%02d:%02d", hour, minute, second)

            val broadcastIntent = Intent(TIMER_BROADCAST)
            broadcastIntent.action = ACTION_STATE_CHANGED
            broadcastIntent.putExtra(INTENT_CURRENT_TIME, time)
            sendBroadcast(broadcastIntent)
        }
    }

    private val mTimer = object : Runnable {
        override fun run() {
            updateTimer()
            mHandler.postDelayed(this, REFRESH_DELAY)
        }
    }

    private fun showNotif() {
        val notification =
            NotificationUtil.createNotification(this, getString(R.string.initial_time), false)
        startForeground(KEY_NOTIFICATION_ID, notification)
    }

    private fun stopNotif() {
        stopForeground(false)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_NOTIFICATION_PLAY -> onPlay()
                ACTION_STATE_CHANGED -> {
                    if (intent.hasExtra(INTENT_CURRENT_TIME)) {
                        val time = intent.getStringExtra(INTENT_CURRENT_TIME) as String
                        NotificationUtil.updateNotification(this@MediaService, time, isPlay)
                    }
                }
            }
        }
    }
}